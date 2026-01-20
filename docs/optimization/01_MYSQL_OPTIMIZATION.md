# MySQL 최적화 가이드

## 개요

MySQL 기반 주차장 검색 API 최적화 기법입니다.

---

## 1. 현재 쿼리 분석

### 현재 구현 (FacilityRepository)

```sql
SELECT f.*,
       (6371 * acos(cos(radians(:lat)) * cos(radians(f.latitude))
       * cos(radians(f.longitude) - radians(:lng))
       + sin(radians(:lat)) * sin(radians(f.latitude)))) AS distance
FROM facility f
WHERE f.type = :type
  AND f.latitude BETWEEN :minLat AND :maxLat
  AND f.longitude BETWEEN :minLng AND :maxLng
  AND (6371 * acos(...)) <= :radius
ORDER BY distance
```

### 문제점

1. **Haversine 공식 중복 계산**: WHERE절과 SELECT절에서 2번 계산
2. **인덱스 미활용**: 복합 인덱스 없음
3. **Full Table Scan**: 대량 데이터에서 성능 저하

---

## 2. 인덱스 최적화

### 2.1 복합 인덱스 생성

```sql
-- 타입 + 위치 복합 인덱스
CREATE INDEX idx_facility_type_location
ON facility(type, latitude, longitude);

-- 가용성 필터용 인덱스
CREATE INDEX idx_facility_type_available
ON facility(type, available_count, latitude, longitude);
```

### 2.2 공간 인덱스 (Spatial Index)

```sql
-- 1. POINT 컬럼 추가
ALTER TABLE facility
ADD COLUMN location_point POINT SRID 4326;

-- 2. 기존 데이터 마이그레이션
UPDATE facility
SET location_point = ST_SRID(POINT(longitude, latitude), 4326);

-- 3. 공간 인덱스 생성
CREATE SPATIAL INDEX idx_facility_location_point
ON facility(location_point);

-- 4. 트리거로 자동 동기화
DELIMITER //
CREATE TRIGGER trg_facility_location_insert
BEFORE INSERT ON facility
FOR EACH ROW
BEGIN
    SET NEW.location_point = ST_SRID(POINT(NEW.longitude, NEW.latitude), 4326);
END//

CREATE TRIGGER trg_facility_location_update
BEFORE UPDATE ON facility
FOR EACH ROW
BEGIN
    IF NEW.latitude != OLD.latitude OR NEW.longitude != OLD.longitude THEN
        SET NEW.location_point = ST_SRID(POINT(NEW.longitude, NEW.latitude), 4326);
    END IF;
END//
DELIMITER ;
```

---

## 3. 쿼리 최적화

### 3.1 ST_Distance_Sphere 사용 (MySQL 8.0+)

**Before (Haversine 수동 계산)**
```sql
SELECT *,
       (6371 * acos(cos(radians(:lat)) * cos(radians(latitude))
       * cos(radians(longitude) - radians(:lng))
       + sin(radians(:lat)) * sin(radians(latitude)))) AS distance
FROM facility
WHERE ...
```

**After (내장 함수)**
```sql
SELECT *,
       ST_Distance_Sphere(
           location_point,
           ST_SRID(POINT(:lng, :lat), 4326)
       ) / 1000 AS distance_km
FROM facility
WHERE ST_Contains(
    ST_Buffer(
        ST_SRID(POINT(:lng, :lat), 4326),
        :radiusMeters
    ),
    location_point
)
ORDER BY distance_km
```

### 3.2 MBR 기반 2단계 필터링

```sql
-- 1단계: Bounding Box로 후보군 축소 (인덱스 활용)
-- 2단계: 정확한 거리 계산

SELECT f.*,
       ST_Distance_Sphere(
           f.location_point,
           ST_SRID(POINT(:lng, :lat), 4326)
       ) / 1000 AS distance_km
FROM facility f
WHERE f.type = :type
  -- 1단계: MBR 필터 (공간 인덱스 사용)
  AND MBRContains(
      ST_Buffer(ST_SRID(POINT(:lng, :lat), 4326), :radiusKm * 1000 / 111000),
      f.location_point
  )
  -- 2단계: 정확한 거리 필터
  AND ST_Distance_Sphere(
      f.location_point,
      ST_SRID(POINT(:lng, :lat), 4326)
  ) <= :radiusKm * 1000
ORDER BY distance_km
LIMIT :size OFFSET :offset;
```

### 3.3 Repository 코드 수정

```java
@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    @Query(value = """
        SELECT f.*,
               ST_Distance_Sphere(
                   f.location_point,
                   ST_SRID(POINT(:lng, :lat), 4326)
               ) / 1000 AS distance
        FROM facility f
        WHERE f.type = :type
          AND f.latitude BETWEEN :minLat AND :maxLat
          AND f.longitude BETWEEN :minLng AND :maxLng
          AND ST_Distance_Sphere(
                  f.location_point,
                  ST_SRID(POINT(:lng, :lat), 4326)
              ) <= :radiusMeters
        ORDER BY distance
        """, nativeQuery = true)
    List<Object[]> findByLocationWithSpatialIndex(
        @Param("type") String type,
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLng") double minLng,
        @Param("maxLng") double maxLng,
        @Param("radiusMeters") double radiusMeters,
        Pageable pageable
    );
}
```

---

## 4. Connection Pool 최적화

### application.yml

```yaml
spring:
  datasource:
    hikari:
      # 기본 설정
      maximum-pool-size: 20          # 최대 커넥션 수
      minimum-idle: 5                # 최소 유휴 커넥션
      idle-timeout: 300000           # 유휴 커넥션 타임아웃 (5분)
      max-lifetime: 1800000          # 커넥션 최대 수명 (30분)
      connection-timeout: 30000      # 커넥션 획득 타임아웃 (30초)

      # 성능 최적화
      leak-detection-threshold: 60000  # 커넥션 누수 감지 (1분)
      validation-timeout: 5000         # 유효성 검사 타임아웃

      # 쿼리 최적화
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
```

### Pool Size 계산 공식

```
최적 Pool Size = (코어 수 * 2) + 유효 스핀들 수

예시:
- 4 Core CPU, SSD 사용
- Pool Size = (4 * 2) + 1 = 9 ~ 10
```

---

## 5. 쿼리 실행 계획 분석

### EXPLAIN 사용

```sql
EXPLAIN ANALYZE
SELECT f.*,
       ST_Distance_Sphere(location_point, ST_SRID(POINT(127.0, 37.5), 4326)) / 1000 AS distance
FROM facility f
WHERE f.type = 'PARKING'
  AND f.latitude BETWEEN 37.4 AND 37.6
  AND f.longitude BETWEEN 126.9 AND 127.1
ORDER BY distance
LIMIT 20;
```

### 예상 결과 (최적화 전)

```
+----+-------------+-------+------+---------------+------+---------+------+-------+-----------------------------+
| id | select_type | table | type | possible_keys | key  | key_len | ref  | rows  | Extra                       |
+----+-------------+-------+------+---------------+------+---------+------+-------+-----------------------------+
|  1 | SIMPLE      | f     | ALL  | NULL          | NULL | NULL    | NULL | 50000 | Using where; Using filesort |
+----+-------------+-------+------+---------------+------+---------+------+-------+-----------------------------+
```

### 예상 결과 (최적화 후)

```
+----+-------------+-------+-------+---------------------------+---------------------------+---------+------+------+-------------+
| id | select_type | table | type  | possible_keys             | key                       | key_len | ref  | rows | Extra       |
+----+-------------+-------+-------+---------------------------+---------------------------+---------+------+------+-------------+
|  1 | SIMPLE      | f     | range | idx_facility_type_location| idx_facility_type_location| 17      | NULL | 500  | Using where |
+----+-------------+-------+-------+---------------------------+---------------------------+---------+------+------+-------------+
```

---

## 6. 체크리스트

- [ ] 복합 인덱스 생성 (`type`, `latitude`, `longitude`)
- [ ] POINT 컬럼 및 공간 인덱스 추가
- [ ] ST_Distance_Sphere 함수 적용
- [ ] HikariCP 설정 튜닝
- [ ] 슬로우 쿼리 로깅 활성화
- [ ] EXPLAIN ANALYZE로 실행 계획 검증
- [ ] JMeter로 성능 측정

---

## 7. 참고 자료

- [MySQL 8.0 Spatial Functions](https://dev.mysql.com/doc/refman/8.0/en/spatial-analysis-functions.html)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [MySQL Index Optimization](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html)
