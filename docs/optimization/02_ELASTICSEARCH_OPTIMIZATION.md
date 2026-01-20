# ElasticSearch 최적화 가이드

## 개요

ElasticSearch 기반 주차장 검색 API 최적화 기법입니다.

---

## 1. 현재 구현 분석

### 현재 쿼리 (ParkingSearchRepository)

```java
BoolQuery.Builder boolQuery = new BoolQuery.Builder()
    .filter(f -> f.term(t -> t.field("type").value("PARKING")))
    .filter(f -> f.geoDistance(g -> g
        .field("location")
        .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
        .distance(radiusKm + "km")));
```

### 현재 장점
- Filter Context 사용 (캐싱 가능)
- Geo Distance 쿼리 사용

### 개선 포인트
- Routing 미설정
- Shard 수 기본값
- Geo-hash 사전 집계 없음

---

## 2. 인덱스 매핑 최적화

### 2.1 최적화된 매핑

```json
PUT /facilities
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "5s",
    "index": {
      "sort.field": ["type", "updatedAt"],
      "sort.order": ["asc", "desc"]
    }
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "keyword",
        "doc_values": true
      },
      "externalId": {
        "type": "keyword"
      },
      "type": {
        "type": "keyword"
      },
      "name": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "address": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "location": {
        "type": "geo_point"
      },
      "geohashPrefix": {
        "type": "keyword"
      },
      "region": {
        "type": "keyword"
      },
      "totalCount": {
        "type": "integer"
      },
      "availableCount": {
        "type": "integer"
      },
      "extraInfo": {
        "type": "object",
        "enabled": false
      },
      "collectedAt": {
        "type": "date"
      },
      "updatedAt": {
        "type": "date"
      }
    }
  }
}
```

### 2.2 필드별 최적화 포인트

| 필드 | 타입 | 최적화 |
|------|------|--------|
| `type` | keyword | Filter 전용, 캐싱 가능 |
| `location` | geo_point | Geo 쿼리 최적화 |
| `geohashPrefix` | keyword | 사전 계산된 geohash |
| `extraInfo` | object (disabled) | 인덱싱 비활성화, 저장만 |

---

## 3. Routing 설정

### 3.1 지역별 Routing

```json
PUT /facilities/_doc/1?routing=seoul_gangnam
{
  "name": "강남역 주차장",
  "region": "seoul_gangnam",
  ...
}
```

### 3.2 검색 시 Routing 지정

```java
// 검색 시 특정 샤드만 조회
SearchRequest request = SearchRequest.of(s -> s
    .index("facilities")
    .routing("seoul_gangnam")  // 해당 지역 샤드만 검색
    .query(q -> q.bool(boolQuery.build()))
);
```

### 3.3 Routing 키 생성 로직

```java
public class GeoRoutingUtils {

    // 위도/경도를 region 키로 변환
    public static String getRegion(double lat, double lng) {
        // 서울 구 단위 매핑
        if (lat >= 37.49 && lat <= 37.53 && lng >= 127.02 && lng <= 127.06) {
            return "seoul_gangnam";
        }
        // ... 기타 지역

        // 기본: geohash 4자리 (약 20km)
        return GeoHash.encodeHash(lat, lng, 4);
    }
}
```

---

## 4. Shard 최적화

### 4.1 Shard 수 결정 기준

```
권장 Shard 크기: 10GB ~ 50GB
권장 Shard 수: ceil(예상 데이터 크기 / 30GB)

예시:
- 주차장 10만 건, 문서당 1KB
- 총 크기: 100MB
- Shard 수: 1 (소규모)

대규모 (100만 건):
- 총 크기: 1GB
- Shard 수: 1~3
```

### 4.2 Shard 재조정

```json
// 현재 인덱스 설정 확인
GET /facilities/_settings

// 새 인덱스로 Reindex (Shard 수 변경)
PUT /facilities_v2
{
  "settings": {
    "number_of_shards": 3
  }
}

POST /_reindex
{
  "source": { "index": "facilities" },
  "dest": { "index": "facilities_v2" }
}

// Alias 전환
POST /_aliases
{
  "actions": [
    { "remove": { "index": "facilities", "alias": "facilities_alias" }},
    { "add": { "index": "facilities_v2", "alias": "facilities_alias" }}
  ]
}
```

---

## 5. Geo-Hash 사전 집계

### 5.1 문서 인덱싱 시 Geohash 추가

```java
@Service
public class FacilityIndexingService {

    public void indexFacility(Facility facility) {
        ParkingDocument doc = new ParkingDocument();
        doc.setLocation(new GeoPoint(facility.getLatitude(), facility.getLongitude()));

        // Geohash 사전 계산 (5자리 = 약 5km 정밀도)
        String geohash = GeoHash.encodeHash(
            facility.getLatitude(),
            facility.getLongitude(),
            5
        );
        doc.setGeohashPrefix(geohash);

        // 지역 정보도 추가
        doc.setRegion(GeoRoutingUtils.getRegion(
            facility.getLatitude(),
            facility.getLongitude()
        ));

        repository.save(doc);
    }
}
```

### 5.2 Geohash 기반 빠른 검색

```java
public List<ParkingDocument> searchByGeohash(double lat, double lng, double radiusKm) {
    // 검색 중심점의 geohash
    String centerGeohash = GeoHash.encodeHash(lat, lng, 5);

    // 인접 geohash들 계산 (9개: 중심 + 8방향)
    Set<String> neighborHashes = GeoHash.getNeighbors(centerGeohash);
    neighborHashes.add(centerGeohash);

    // 1단계: geohash로 빠른 필터링
    BoolQuery.Builder boolQuery = new BoolQuery.Builder()
        .filter(f -> f.terms(t -> t
            .field("geohashPrefix")
            .terms(ts -> ts.value(neighborHashes.stream()
                .map(FieldValue::of)
                .toList()))))
        // 2단계: 정확한 거리 필터
        .filter(f -> f.geoDistance(g -> g
            .field("location")
            .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
            .distance(radiusKm + "km")));

    return executeSearch(boolQuery.build());
}
```

---

## 6. 쿼리 최적화

### 6.1 Filter Context 활용 (현재 적용됨)

```java
// Good: Filter Context (캐싱 가능, 스코어링 없음)
BoolQuery.Builder query = new BoolQuery.Builder()
    .filter(f -> f.term(t -> t.field("type").value("PARKING")))
    .filter(f -> f.geoDistance(...));

// Bad: Must Context (캐싱 불가, 스코어링 계산)
BoolQuery.Builder query = new BoolQuery.Builder()
    .must(m -> m.term(t -> t.field("type").value("PARKING")));
```

### 6.2 Source Filtering

```java
SearchRequest request = SearchRequest.of(s -> s
    .index("facilities")
    .source(src -> src
        .filter(f -> f
            .includes("id", "name", "address", "location",
                     "totalCount", "availableCount", "extraInfo")
            .excludes("collectedAt")  // 불필요한 필드 제외
        ))
    .query(q -> q.bool(boolQuery.build()))
);
```

### 6.3 Pagination 최적화

```java
// 얕은 페이지 (page < 100): from/size 사용
SearchRequest request = SearchRequest.of(s -> s
    .index("facilities")
    .from(page * size)
    .size(size)
    .query(query)
);

// 깊은 페이지 (page >= 100): search_after 사용
SearchRequest request = SearchRequest.of(s -> s
    .index("facilities")
    .size(size)
    .searchAfter(lastSortValues)  // 이전 결과의 sort 값
    .sort(sort -> sort.geoDistance(g -> g
        .field("location")
        .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
        .order(SortOrder.Asc)))
    .query(query)
);
```

---

## 7. 클러스터 설정

### 7.1 JVM Heap 설정

```yaml
# jvm.options
-Xms4g
-Xmx4g  # 전체 메모리의 50% 이하, 최대 32GB
```

### 7.2 Thread Pool 설정

```yaml
# elasticsearch.yml
thread_pool:
  search:
    size: 13  # (코어 수 * 3 / 2) + 1
    queue_size: 1000
```

### 7.3 Circuit Breaker 설정

```yaml
indices.breaker.total.limit: 70%
indices.breaker.fielddata.limit: 40%
indices.breaker.request.limit: 40%
```

---

## 8. 모니터링 쿼리

### 8.1 인덱스 상태 확인

```bash
# 인덱스 크기 및 문서 수
GET /_cat/indices/facilities?v

# 샤드 분배 확인
GET /_cat/shards/facilities?v

# 세그먼트 정보
GET /facilities/_segments
```

### 8.2 쿼리 성능 프로파일링

```json
GET /facilities/_search
{
  "profile": true,
  "query": {
    "bool": {
      "filter": [
        { "term": { "type": "PARKING" }},
        { "geo_distance": {
            "distance": "5km",
            "location": { "lat": 37.5, "lon": 127.0 }
        }}
      ]
    }
  }
}
```

---

## 9. 체크리스트

- [ ] 인덱스 매핑 최적화 적용
- [ ] Geohash prefix 필드 추가
- [ ] Region 필드 추가 및 Routing 설정
- [ ] Shard 수 조정 (데이터 크기 기반)
- [ ] Source Filtering 적용
- [ ] 깊은 페이지네이션 search_after 적용
- [ ] 쿼리 프로파일링으로 병목 확인
- [ ] JMeter로 성능 측정

---

## 10. 참고 자료

- [Elasticsearch Geo Queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-queries.html)
- [Elasticsearch Tuning for Search Speed](https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-search-speed.html)
- [Geohash Grid Aggregation](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-geohashgrid-aggregation.html)
