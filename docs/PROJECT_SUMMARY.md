# 전기차 충전소 통합 검색 시스템

> DDD 기반 설계와 이벤트 드리븐 아키텍처를 적용한 실시간 충전소 검색 플랫폼

---

## 목차
1. [요구사항](#1-요구사항)
2. [설계](#2-설계)
3. [구현](#3-구현)
4. [결론](#4-결론)

---

# 1. 요구사항

## 1.1 프로젝트 배경

전기차 보급이 급속히 확대되면서 충전 인프라에 대한 실시간 정보 제공의 필요성이 증가하고 있습니다. 사용자들은 현재 위치에서 가장 가까운 충전소를 찾고, 실시간 가용 현황을 확인하며, 다양한 조건으로 필터링하여 최적의 충전소를 선택하고자 합니다.

본 프로젝트는 **한국환경공단 전기차 충전소 공공 API**를 활용하여 전국 충전소 데이터를 수집하고, 이를 기반으로 위치 기반 검색 서비스를 제공하는 시스템을 구축합니다.

## 1.2 기능 요구사항

### 핵심 기능

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| **위치 기반 검색** | 사용자 위치 기준 반경 내 충전소 검색 | 필수 |
| **실시간 가용성 조회** | 충전기 사용 가능 여부 실시간 확인 | 필수 |
| **다중 조건 필터링** | 충전기 타입, 주차 무료 여부 등 필터 | 필수 |
| **거리순 정렬** | 현재 위치에서 가까운 순으로 정렬 | 필수 |
| **충전소 상세 정보** | 운영시간, 충전 용량, 운영기관 등 상세 정보 | 필수 |
| **통계 조회** | 지역별, 타입별 충전소 현황 통계 | 선택 |

### API 명세

```
GET  /api/v1/chargers/search     - 충전소 검색 (위치 기반)
GET  /api/v1/chargers/{id}       - 충전소 상세 조회
GET  /api/v1/chargers/nearby     - 내 주변 충전소 (거리순)
GET  /api/v1/chargers/available  - 사용 가능한 충전소
GET  /api/v1/chargers/stats      - 충전소 통계
```

## 1.3 비기능 요구사항

| 항목 | 요구사항 | 목표치 |
|------|----------|--------|
| **응답 시간** | 검색 API 응답 | < 200ms |
| **데이터 신선도** | 공공 API 동기화 주기 | 5분 |
| **확장성** | 대용량 데이터 처리 | 10만+ 건 |
| **가용성** | 시스템 가동률 | 99.9% |
| **유지보수성** | 모듈 간 결합도 | 낮음 (느슨한 결합) |

## 1.4 데이터 소스

**한국환경공단 전기차 충전소 API**
- 제공 정보: 충전소명, 위치, 충전기 타입, 충전 용량, 실시간 상태
- 데이터 갱신: 실시간 (API 호출 시점 기준)
- 전국 충전소 약 20만+ 건

---

# 2. 설계

## 2.1 도메인 주도 설계 (DDD)

### 2.1.1 유비쿼터스 언어 정의

도메인 전문가와 개발자가 공통으로 사용하는 용어를 명확히 정의하여 커뮤니케이션 오류를 방지합니다.

| 한글 | 영문 | 정의 |
|------|------|------|
| 시설 | Facility | 주차장 또는 충전소를 총칭하는 상위 개념 |
| 충전소 | ChargingStation | 전기차를 충전할 수 있는 시설 |
| 충전기 | Charger | 충전소 내 개별 충전 장치 |
| 가용성 | Availability | 현재 이용 가능한 충전기 수 |
| 점유율 | OccupancyRate | 사용 중인 충전기 비율 |
| 외부 식별자 | ExternalId | 공공 API에서 부여한 고유 ID |

### 2.1.2 도메인 분류

```
┌─────────────────────────────────────────────────────────────┐
│                    핵심 도메인 (Core)                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              시설 검색 도메인                         │   │
│  │  - 위치 기반 시설 검색                               │   │
│  │  - 실시간 가용성 조회                                │   │
│  │  - 다중 조건 필터링                                  │   │
│  └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                  지원 도메인 (Supporting)                    │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │   데이터 수집 도메인   │  │   시설 관리 도메인   │        │
│  │  - 외부 API 연동      │  │  - 시설 정보 저장    │        │
│  │  - 데이터 변환        │  │  - 상태 갱신         │        │
│  │  - 스케줄링           │  │  - 이력 관리         │        │
│  └──────────────────────┘  └──────────────────────┘        │
├─────────────────────────────────────────────────────────────┤
│                   일반 도메인 (Generic)                      │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │    위치 서비스        │  │    메시징 서비스     │        │
│  │  - 좌표/거리 계산     │  │  - 이벤트 발행/구독  │        │
│  └──────────────────────┘  └──────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### 2.1.3 바운디드 컨텍스트

각 컨텍스트는 명확한 경계를 가지며, 자체 모델과 언어를 사용합니다.

```
┌────────────────┐      ┌────────────────┐      ┌────────────────┐
│   Collection   │      │   Processing   │      │      API       │
│    Context     │─────▶│    Context     │─────▶│    Context     │
├────────────────┤      ├────────────────┤      ├────────────────┤
│ 책임:          │      │ 책임:          │      │ 책임:          │
│ - API 호출     │      │ - 이벤트 소비  │      │ - REST API     │
│ - 데이터 수집  │      │ - DB 저장      │      │ - 검색 처리    │
│ - 이벤트 발행  │      │ - ES 인덱싱    │      │ - 응답 변환    │
├────────────────┤      ├────────────────┤      ├────────────────┤
│ 모델:          │      │ 모델:          │      │ 모델:          │
│ - ApiResponse  │      │ - Facility     │      │ - SearchReq    │
│ - RawData      │      │ - Availability │      │ - ChargerRes   │
└────────────────┘      └────────────────┘      └────────────────┘
        │                       │                       │
        └───────────────────────┴───────────────────────┘
                         common 모듈
                    (Shared Kernel: 핵심 도메인 모델)
```

### 2.1.4 컨텍스트 맵 및 관계

```
┌──────────────┐                    ┌──────────────┐
│  External    │   Conformist       │  Collection  │
│  Public API  │ ─────────────────▶ │   Context    │
└──────────────┘   (ACL 적용)       └──────┬───────┘
                                           │
                                           │ Published Language
                                           │ (Kafka 이벤트)
                                           ▼
                                    ┌──────────────┐
                                    │  Processing  │
                                    │   Context    │
                                    └──────┬───────┘
                                           │
                                           │ Shared Kernel
                                           │ (common 모듈)
                                           ▼
                                    ┌──────────────┐
                                    │     API      │
                                    │   Context    │
                                    └──────────────┘
```

**관계 유형:**
- **Conformist (순응자)**: 외부 API 스키마를 수용하되, ACL로 내부 도메인 보호
- **Published Language**: FacilityEvent를 통한 표준화된 이벤트 교환
- **Shared Kernel**: common 모듈을 통해 핵심 도메인 모델 공유

### 2.1.5 Anti-Corruption Layer (ACL)

외부 공공 API의 스키마 변경으로부터 내부 도메인을 보호하는 변환 계층입니다.

```java
// 외부 API 응답 (공공 API 스키마)
{
  "statNm": "서울시청 충전소",
  "statId": "ME000001",
  "lat": "37.5666",
  "lng": "126.9784",
  "stat": "2",           // 상태 코드
  "chgerType": "07"      // 충전기 타입 코드
}

        │
        │  FacilityTranslator (ACL)
        ▼

// 내부 도메인 모델
Facility {
  externalId: "ME000001",
  name: "서울시청 충전소",
  location: Location(37.5666, 126.9784),
  availability: Availability(total=1, available=1),
  type: FacilityType.CHARGING
}
```

### 2.1.6 전술적 설계 - 애그리거트 & 값 객체

**Facility Aggregate (루트 엔티티)**

```
┌─────────────────────────────────────────────────────────┐
│                   Facility (Aggregate Root)              │
├─────────────────────────────────────────────────────────┤
│  - id: Long                                             │
│  - externalId: String (불변)                            │
│  - type: FacilityType                                   │
│  - name: String                                         │
│  ┌─────────────────┐  ┌─────────────────┐              │
│  │    Location     │  │   Availability  │              │
│  │   (Value Object)│  │  (Value Object) │              │
│  ├─────────────────┤  ├─────────────────┤              │
│  │ - latitude      │  │ - totalCount    │              │
│  │ - longitude     │  │ - availableCount│              │
│  │ + distanceTo()  │  │ + occupancyRate │              │
│  │ + isValid()     │  │ + isFull()      │              │
│  └─────────────────┘  │ + congestionLv  │              │
│                       └─────────────────┘              │
│  - extraInfo: JSON (충전기 상세 정보)                   │
│  - collectedAt: LocalDateTime                          │
├─────────────────────────────────────────────────────────┤
│  + createCharging()   // 팩토리 메서드                  │
│  + updateAvailability()                                 │
│  + updateFrom()                                         │
│  + distanceFrom()                                       │
└─────────────────────────────────────────────────────────┘
```

**값 객체의 불변식 (Invariants)**

```java
// Location - 좌표 유효성 보장
public Location(double latitude, double longitude) {
    if (latitude < -90 || latitude > 90)
        throw new IllegalArgumentException("위도는 -90~90 범위");
    if (longitude < -180 || longitude > 180)
        throw new IllegalArgumentException("경도는 -180~180 범위");
}

// Availability - 가용성 일관성 보장
public Availability(int totalCount, int availableCount) {
    if (totalCount < 0)
        throw new IllegalArgumentException("총 수량은 0 이상");
    if (availableCount < 0 || availableCount > totalCount)
        throw new IllegalArgumentException("가용 수량은 0~총수량 범위");
}
```

## 2.2 시스템 아키텍처

### 2.2.1 전체 아키텍처

```
                              ┌─────────────────────────────────────┐
                              │           Client (Mobile/Web)       │
                              └──────────────────┬──────────────────┘
                                                 │
                                                 ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                                API Module                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐         │
│  │ ChargerController│  │  ChargerService  │  │ChargerSearchRepo │         │
│  │                  │  │                  │  │ (ElasticSearch)  │         │
│  │ - /search        │─▶│ - search()       │─▶│ - geo_distance   │         │
│  │ - /nearby        │  │ - findNearby()   │  │ - bool query     │         │
│  │ - /available     │  │ - findAvailable()│  │ - aggregation    │         │
│  │ - /stats         │  │ - getStats()     │  │                  │         │
│  └──────────────────┘  └──────────────────┘  └────────┬─────────┘         │
└────────────────────────────────────────────────────────┼───────────────────┘
                                                         │
                    ┌────────────────────────────────────┼────────────────┐
                    │                                    ▼                │
                    │  ┌─────────────────────────────────────────────┐   │
                    │  │              ElasticSearch                   │   │
                    │  │  - facilities index                         │   │
                    │  │  - geo_point mapping                        │   │
                    │  │  - 실시간 검색 최적화                        │   │
                    │  └─────────────────────────────────────────────┘   │
                    │                        ▲                           │
                    │                        │ 인덱싱                    │
┌───────────────────┼────────────────────────┼───────────────────────────┼───┐
│                   │     Processing Module  │                           │   │
│  ┌────────────────┴──────┐  ┌──────────────┴─────┐  ┌────────────────┐│   │
│  │ FacilityEventConsumer │  │   IndexingService  │  │ FacilityService││   │
│  │                       │  │                    │  │                ││   │
│  │ @KafkaListener        │─▶│ - indexFacility()  │  │ - upsert()     ││   │
│  │ - facility-events     │  │ - GeoPoint 변환    │  │ - DB 저장      ││   │
│  └───────────────────────┘  └────────────────────┘  └───────┬────────┘│   │
└─────────────────────────────────────────────────────────────┼─────────┘   │
                    ▲                                         │             │
                    │ Kafka                                   ▼             │
                    │                              ┌─────────────────────┐  │
┌───────────────────┼──────────────────────────────│       MySQL         │  │
│  Collection Module│                              │  - facility 테이블  │  │
│  ┌────────────────┴──────┐  ┌──────────────────┐ │  - parking_session  │  │
│  │  CollectionScheduler  │  │  ParkingApiClient│ └─────────────────────┘  │
│  │                       │  │                  │                          │
│  │  @Scheduled(5분)      │─▶│ - 공공 API 호출  │                          │
│  │  - collectChargers()  │  │ - HTTP Client    │                          │
│  └───────────────────────┘  └────────┬─────────┘                          │
│                                      │                                    │
│  ┌───────────────────────┐  ┌────────▼─────────┐                          │
│  │ FacilityEventProducer │◀─│FacilityTranslator│                          │
│  │                       │  │     (ACL)        │                          │
│  │ - Kafka 발행          │  │ - 외부→내부 변환 │                          │
│  └───────────────────────┘  └──────────────────┘                          │
└───────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────────────────────────┐
                              │        한국환경공단 공공 API         │
                              │   (전기차 충전소 정보 조회 서비스)    │
                              └─────────────────────────────────────┘
```

### 2.2.2 데이터 파이프라인

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  공공 API   │────▶│ Collection  │────▶│   Kafka     │────▶│ Processing  │
│             │     │   Module    │     │             │     │   Module    │
└─────────────┘     └─────────────┘     └─────────────┘     └──────┬──────┘
                                                                   │
                                                    ┌──────────────┼──────────────┐
                                                    │              │              │
                                                    ▼              ▼              ▼
                                              ┌──────────┐  ┌──────────┐  ┌──────────┐
                                              │  MySQL   │  │ElasticS  │  │   API    │
                                              │  (원본)  │  │ (검색)   │  │  Module  │
                                              └──────────┘  └──────────┘  └──────────┘
```

**데이터 흐름 상세:**

1. **수집 (Collection)**: 5분 주기로 공공 API 호출
2. **변환 (ACL)**: 외부 스키마 → 내부 도메인 모델 변환
3. **발행 (Kafka)**: FacilityEvent 발행
4. **처리 (Processing)**: 이벤트 소비 → DB 저장 + ES 인덱싱
5. **검색 (API)**: ElasticSearch geo_distance 쿼리

### 2.2.3 ElasticSearch 인덱스 설계

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "externalId": { "type": "keyword" },
      "type": { "type": "keyword" },
      "name": { "type": "text", "analyzer": "korean" },
      "address": { "type": "text", "analyzer": "korean" },
      "location": { "type": "geo_point" },
      "totalCount": { "type": "integer" },
      "availableCount": { "type": "integer" },
      "extraInfo": { "type": "text" },
      "collectedAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

**geo_point를 사용한 위치 기반 검색:**
- `geo_distance` 쿼리: 반경 내 검색
- `geo_distance` 정렬: 거리순 정렬
- 인덱스 레벨에서 공간 연산 처리 → 고성능

---

# 3. 구현

## 3.1 기술 스택

| 구분 | 기술 | 버전 | 선정 이유 |
|------|------|------|-----------|
| **Language** | Java | 17 | LTS, Record/Sealed 클래스 지원 |
| **Framework** | Spring Boot | 3.2.1 | 최신 안정 버전, Native 지원 |
| **Build** | Gradle | 8.x | 멀티모듈 관리, 빠른 빌드 |
| **Database** | MySQL | 8.0 | 트랜잭션, JSON 컬럼 지원 |
| **Search Engine** | ElasticSearch | 8.x | geo_point, 분산 검색 |
| **Message Queue** | Apache Kafka | 3.x | 고가용성, 이벤트 소싱 |
| **ORM** | Spring Data JPA | - | Repository 추상화 |
| **Container** | Docker Compose | - | 로컬 개발 환경 |

## 3.2 모듈 구조

```
parking-charging/
├── settings.gradle          # 멀티모듈 설정
├── build.gradle             # 루트 빌드 설정
├── docker-compose.yml       # 인프라 구성
│
├── common/                  # 공유 도메인 모델
│   └── src/main/java/com/example/common/
│       ├── domain/
│       │   ├── FacilityType.java
│       │   ├── entity/
│       │   │   ├── Facility.java        # Aggregate Root
│       │   │   ├── ParkingSession.java
│       │   │   └── Payment.java
│       │   └── vo/
│       │       ├── Location.java        # 위치 VO
│       │       ├── Availability.java    # 가용성 VO
│       │       ├── Money.java           # 금액 VO
│       │       └── VehicleNumber.java   # 차량번호 VO
│       ├── event/
│       │   └── FacilityEvent.java       # 도메인 이벤트
│       └── util/
│           └── GeoUtils.java            # 거리 계산
│
├── collection/              # 데이터 수집 모듈
│   └── src/main/java/com/example/collection/
│       ├── client/
│       │   └── ParkingApiClient.java    # 공공 API 클라이언트
│       ├── dto/
│       │   └── ChargingApiResponse.java # API 응답 DTO
│       ├── translator/
│       │   └── FacilityTranslator.java  # ACL (변환 계층)
│       ├── producer/
│       │   └── FacilityEventProducer.java
│       └── scheduler/
│           └── CollectionScheduler.java # 5분 주기 스케줄링
│
├── processing/              # 이벤트 처리 모듈
│   └── src/main/java/com/example/processing/
│       ├── consumer/
│       │   └── FacilityEventConsumer.java
│       ├── service/
│       │   ├── FacilityService.java     # DB 저장
│       │   └── IndexingService.java     # ES 인덱싱
│       └── repository/
│           └── FacilityRepository.java
│
└── api/                     # REST API 모듈
    └── src/main/java/com/example/api/
        ├── controller/
        │   ├── ChargerController.java   # 충전소 API
        │   └── ParkingController.java   # 입출차 API
        ├── service/
        │   ├── ChargerService.java      # ES 기반 검색
        │   └── PaymentService.java      # 결제 처리
        ├── repository/
        │   └── ChargerSearchRepository.java  # ES 쿼리
        ├── document/
        │   └── ChargerDocument.java     # ES 문서
        └── dto/
            ├── ChargerSearchRequest.java
            ├── ChargerResponse.java
            └── ChargerStatsResponse.java
```

## 3.3 핵심 구현

### 3.3.1 도메인 모델 - Facility Aggregate

```java
@Entity
@Table(name = "facility")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false)
    private String externalId;  // 불변 - 외부 식별자

    @Enumerated(EnumType.STRING)
    private FacilityType type;

    @Embedded
    private Location location;  // 값 객체

    @Embedded
    private Availability availability;  // 값 객체

    // === 팩토리 메서드 ===
    public static Facility createCharging(String externalId, String name,
            double lat, double lng, String address,
            int total, int available, String extraInfo) {
        return new Facility(externalId, FacilityType.CHARGING, name,
                new Location(lat, lng), address,
                new Availability(total, available), extraInfo);
    }

    // === 도메인 로직 ===
    public void updateAvailability(int total, int available) {
        this.availability = new Availability(total, Math.min(available, total));
        this.updatedAt = LocalDateTime.now();
    }

    public double distanceFrom(double fromLat, double fromLng) {
        return location.distanceTo(new Location(fromLat, fromLng));
    }

    public boolean isFull() {
        return availability.isFull();
    }
}
```

### 3.3.2 값 객체 - Location

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

    private double latitude;
    private double longitude;

    public Location(double latitude, double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Haversine 공식을 이용한 거리 계산
    public double distanceTo(Location other) {
        double R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(this.latitude)) *
                   Math.cos(Math.toRadians(other.latitude)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // 불변식
    private void validateLatitude(double lat) {
        if (lat < -90 || lat > 90)
            throw new IllegalArgumentException("위도는 -90~90 범위여야 합니다");
    }
}
```

### 3.3.3 ACL - FacilityTranslator

```java
@Component
public class FacilityTranslator {

    /**
     * 외부 API 응답 → 내부 도메인 이벤트 변환
     * ACL(Anti-Corruption Layer) 역할
     */
    public FacilityEvent translate(ChargingApiResponse.ChargerItem item) {
        // 외부 상태 코드 → 내부 가용성 변환
        int available = "2".equals(item.getStat()) ? 1 : 0;

        // 외부 extraInfo 구성
        String extraInfo = buildExtraInfo(item);

        return FacilityEvent.builder()
                .eventType(FacilityEvent.EventType.UPSERT)
                .facilityType(FacilityType.CHARGING)
                .externalId(item.getStatId() + "_" + item.getChgerId())
                .name(item.getStatNm())
                .latitude(parseDouble(item.getLat()))
                .longitude(parseDouble(item.getLng()))
                .address(item.getAddr())
                .totalCount(1)
                .availableCount(available)
                .extraInfo(extraInfo)
                .collectedAt(LocalDateTime.now())
                .build();
    }
}
```

### 3.3.4 ElasticSearch 검색 - ChargerSearchRepository

```java
@Repository
@RequiredArgsConstructor
public class ChargerSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 위치 기반 충전소 검색 (geo_distance 쿼리)
     */
    public SearchHits<ChargerDocument> search(
            Double lat, Double lng, double radiusKm,
            String chgerType, Boolean available, int page, int size) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 충전소 타입 필터
        boolBuilder.filter(Query.of(q -> q
                .term(t -> t.field("type").value("CHARGING"))));

        // geo_distance 필터 - 반경 내 검색
        if (lat != null && lng != null) {
            boolBuilder.filter(Query.of(q -> q
                    .geoDistance(g -> g
                            .field("location")
                            .distance(radiusKm + "km")
                            .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                            .distanceType(GeoDistanceType.Arc))));
        }

        // 가용성 필터
        if (available != null && available) {
            boolBuilder.filter(Query.of(q -> q
                    .range(r -> r.field("availableCount").gt(JsonData.of(0)))));
        }

        // 거리순 정렬
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                .withSort(s -> s
                        .geoDistance(g -> g
                                .field("location")
                                .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                                .order(SortOrder.Asc)
                                .unit(DistanceUnit.Kilometers)))
                .withPageable(PageRequest.of(page, size))
                .build();

        return elasticsearchOperations.search(query, ChargerDocument.class);
    }
}
```

### 3.3.5 이벤트 기반 처리

```java
// Producer (Collection Module)
@Component
@RequiredArgsConstructor
public class FacilityEventProducer {
    private final KafkaTemplate<String, FacilityEvent> kafkaTemplate;

    public void send(FacilityEvent event) {
        kafkaTemplate.send("facility-events", event.getExternalId(), event);
    }
}

// Consumer (Processing Module)
@Component
@RequiredArgsConstructor
public class FacilityEventConsumer {
    private final FacilityService facilityService;
    private final IndexingService indexingService;

    @KafkaListener(topics = "facility-events")
    public void consume(FacilityEvent event) {
        // 1. MySQL 저장
        Facility facility = facilityService.upsert(event);

        // 2. ElasticSearch 인덱싱
        indexingService.indexFacility(facility);
    }
}
```

## 3.4 인프라 구성

### docker-compose.yml

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3307:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: parking_charging

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    ports:
      - "9200:9200"
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

---

# 4. 결론

## 4.1 프로젝트 성과

### 아키텍처 측면

| 항목 | 성과 |
|------|------|
| **느슨한 결합** | 모듈 간 Kafka 이벤트 기반 통신으로 독립적 배포 가능 |
| **확장성** | ElasticSearch 분산 검색으로 대용량 데이터 처리 |
| **유지보수성** | DDD 기반 명확한 도메인 경계로 변경 영향 최소화 |
| **데이터 일관성** | 이벤트 소싱 패턴으로 데이터 흐름 추적 가능 |

### 기술적 성과

| 항목 | 적용 기술 | 효과 |
|------|-----------|------|
| **위치 검색** | ES geo_distance | 메모리 O(n) → 인덱스 O(log n) |
| **데이터 수집** | Kafka 비동기 처리 | 수집/처리 분리, 장애 격리 |
| **도메인 보호** | ACL 패턴 | 외부 API 변경 영향 차단 |
| **코드 품질** | 값 객체 불변식 | 도메인 규칙 자동 검증 |

## 4.2 DDD 적용 효과

### Before (기존 방식)
```
Controller → Service → Repository → DB
    └── 모든 비즈니스 로직이 Service에 집중
    └── 도메인 개념이 코드에 드러나지 않음
    └── 외부 API 변경 시 전체 수정 필요
```

### After (DDD 적용)
```
Controller → Application Service → Domain Model → Repository
                                        │
                                   ┌────┴────┐
                                   │ Location│ ← 거리 계산 로직
                                   │Availab. │ ← 점유율/혼잡도
                                   │   ACL   │ ← 외부 변환 격리
                                   └─────────┘
```

**효과:**
1. **유비쿼터스 언어**: 코드가 곧 문서 (Facility, Availability, Location)
2. **도메인 로직 캡슐화**: 값 객체 내 비즈니스 규칙 응집
3. **변경 영향 최소화**: ACL로 외부 변경 격리
4. **테스트 용이성**: 순수 도메인 로직 단위 테스트 가능

## 4.3 ElasticSearch 활용 효과

### 메모리 기반 검색 vs ElasticSearch

```java
// Before: 메모리에서 전체 로드 후 필터링
List<Facility> all = repository.findByType(CHARGING);  // 전체 로드
return all.stream()
    .filter(f -> distance(f, lat, lng) <= radius)  // O(n) 필터
    .sorted(comparing(f -> distance(f, lat, lng))) // O(n log n) 정렬
    .limit(20)
    .toList();

// After: ElasticSearch 인덱스 레벨 처리
SearchHits<ChargerDocument> hits = esOperations.search(
    NativeQuery.builder()
        .withQuery(geoDistance("location", lat, lng, radius))  // 인덱스 필터
        .withSort(geoDistanceSort("location", lat, lng))       // 인덱스 정렬
        .withPageable(PageRequest.of(0, 20))
        .build());
```

| 비교 항목 | 메모리 기반 | ElasticSearch |
|-----------|-------------|---------------|
| 데이터 로드 | 전체 (n건) | 결과만 (20건) |
| 필터링 | O(n) | O(log n) 인덱스 |
| 정렬 | O(n log n) | O(log n) 인덱스 |
| 메모리 사용 | O(n) | O(1) |
| 확장성 | 단일 노드 | 분산 클러스터 |

## 4.4 향후 개선 방향

### 단기
- [ ] Redis 캐싱 레이어 추가 (인기 검색 결과)
- [ ] API Rate Limiting 적용
- [ ] 모니터링 대시보드 구축 (Grafana + Prometheus)

### 중기
- [ ] 주차장 API 연동 확대
- [ ] 사용자 즐겨찾기 기능
- [ ] 충전 예약 시스템

### 장기
- [ ] ML 기반 혼잡도 예측
- [ ] 경로 안내 연동
- [ ] 실시간 알림 서비스

---

## 참고 자료

- [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design - Vaughn Vernon](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577)
- [ElasticSearch Geo Queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-queries.html)
- [한국환경공단 전기차 충전소 API](https://www.data.go.kr/data/15076352/openapi.do)
