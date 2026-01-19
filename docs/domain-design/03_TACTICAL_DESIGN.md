# 전술적 설계 (Tactical Design)

---

## 1. 애그리거트 (Aggregate)

### Facility Aggregate (루트)
시설의 핵심 정보를 관리하는 애그리거트 루트.

**속성**
- id: FacilityId (Long) - 내부 식별자
- externalId: ExternalId - 외부 API 식별자
- type: FacilityType - PARKING / CHARGING
- name: FacilityName
- location: Location (Value Object)
- address: Address (Value Object)
- availability: Availability (Value Object)
- collectedAt: Instant

**확장 (주차장)**
- parkingType: ParkingType (노상/노외/기계식)
- parkingFee: ParkingFee (Value Object)
- paymentMethods: Set<PaymentMethod>

**확장 (충전소)**
- chargers: List<Charger> (Entity)
- operatorName: String
- businessId: String

---

## 2. 엔티티 (Entity)

| 엔티티 | 식별자 | 설명 |
|--------|--------|------|
| Facility | FacilityId (Long) | 시설 루트 엔티티 |
| Charger | ChargerId | 충전소 내 개별 충전기 |

---

## 3. 값 객체 (Value Object)

### Location
- latitude: Double (-90 ~ 90)
- longitude: Double (-180 ~ 180)
- 메서드: distanceTo(other), isValid()

### Availability
- totalCount: Int (>= 0)
- availableCount: Int (0 ~ totalCount)
- 메서드: getOccupancyRate(), isFull(), getCongestionLevel()

### ParkingFee
- baseFee: Int (기본 요금)
- baseMinutes: Int (기본 시간)
- unitFee: Int (추가 단위 요금)
- unitMinutes: Int (추가 단위 시간)
- dailyMaxFee: Int? (일 최대 요금)
- 메서드: calculate(minutes), isFree()

---

## 4. 애그리거트 불변식 (Invariants)

### Facility
- externalId는 생성 후 변경 불가
- latitude는 -90 ~ 90 범위
- longitude는 -180 ~ 180 범위
- name은 비어있을 수 없음

### Availability
- availableCount >= 0
- availableCount <= totalCount
- totalCount >= 0

### ParkingFee
- baseFee >= 0
- baseMinutes > 0
- unitFee >= 0
- unitMinutes > 0

### Charger
- output > 0
- 동일 충전소 내 chargerId 중복 불가

---

## 5. 도메인 이벤트 (Domain Event)

### FacilityEvent (Base)
- eventId: UUID
- eventType: EventType (UPSERT, DELETE)
- occurredAt: Instant
- facilityType: FacilityType

### 이벤트 목록

| 이벤트 | 발행 시점 | 구독자 | 처리 |
|--------|-----------|--------|------|
| FacilityDataCollected | 외부 API 수집 완료 | Processing | DB/캐시 갱신 |
| FacilityCreated | 신규 시설 등록 | Search | Geo 인덱스 추가 |
| FacilityUpdated | 시설 정보 변경 | Search | 캐시 무효화 |
| AvailabilityChanged | 가용성 변경 | Search | 실시간 상태 갱신 |

---

## 6. 도메인 서비스 (Domain Service)

| 서비스 | 책임 | 주요 메서드 |
|--------|------|-------------|
| FacilitySearchService | 위치 기반 검색 | searchByRadius(center, radius, filter) |
| AvailabilityCalculator | 가용성/점유율 계산 | calculate(facility), getOccupancyRate() |
| DistanceCalculator | 거리 계산 | calculateDistance(from, to) |
| PricingCalculator | 요금 계산 | estimateFee(parkingLot, duration) |

---

## 7. 리포지토리 (Repository)

### FacilityRepository
```java
Facility findById(Long id);
Facility findByExternalId(String externalId);
Facility save(Facility facility);
List<Facility> findNearby(Location center, double radiusKm, FacilityType type);
```

### 확장 메서드 (주차장)
```java
List<Facility> findByParkingType(ParkingType type);
List<Facility> findFreeParkingLots(Location center, double radiusKm);
```

### 확장 메서드 (충전소)
```java
List<Facility> findByChargerType(ChargerType type);
List<Facility> findByMinOutput(Location center, double radiusKm, int minKw);
```
