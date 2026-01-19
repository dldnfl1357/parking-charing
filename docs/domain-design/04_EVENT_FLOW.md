# 이벤트 플로우 및 읽기 모델

---

## 1. 이벤트 스토밍 결과

### Data Collection Flow

1. Timer (5분 주기) → Command: Collect FacilityData
2. Command 실행 → Event: DataCollected
3. Policy: On DataCollected → Kafka 발행

### Data Processing Flow

1. Kafka 메시지 수신 → Command: Upsert Facility
2. DB 저장 후 분기:
   - 신규인 경우 → Event: FacilityCreated
   - 기존인 경우 → Event: FacilityUpdated
3. Policy: On FacilityChanged
   - Command: Update Geo Index (Redis GEOADD)
   - Command: Invalidate Cache (캐시 삭제)

### Search Flow

1. User Request → Command: Search Facilities
2. Redis Geo 검색 + DB 조회
3. Event: SearchResult Returned

---

## 2. 읽기 모델 (Read Model)

CQRS 패턴의 Query 측 최적화 모델.

### FacilitySummary (검색 결과용)
목록 조회 시 사용하는 경량 모델.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 시설 ID |
| type | FacilityType | PARKING / CHARGING |
| name | String | 시설명 |
| latitude | Double | 위도 |
| longitude | Double | 경도 |
| distance | Double | 검색 중심으로부터 거리 (km) |
| availableCount | Int | 이용 가능 수 |
| totalCount | Int | 전체 수 |
| occupancyRate | Double | 점유율 |
| congestionLevel | String | 혼잡도 (여유/보통/혼잡/만차) |

### FacilityDetail (상세 조회용)
단건 상세 조회 시 사용하는 전체 모델.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 시설 ID |
| externalId | String | 외부 식별자 |
| type | FacilityType | 시설 유형 |
| name | String | 시설명 |
| address | AddressDto | 주소 정보 |
| location | LocationDto | 좌표 정보 |
| availability | AvailabilityDto | 가용성 정보 |
| operatingInfo | OperatingInfoDto | 운영 정보 |
| parkingInfo | ParkingInfoDto | 주차장 전용 정보 (nullable) |
| chargingInfo | ChargingInfoDto | 충전소 전용 정보 (nullable) |
| collectedAt | Instant | 데이터 수집 시각 |
| updatedAt | Instant | 마지막 갱신 시각 |

---

## 3. 데이터 흐름

### 수집 → 저장 흐름
1. Collector Service: 공공 API 호출 (5분 주기)
2. Collector Service: FacilityEvent 생성 → Kafka 발행
3. Processor Service: Kafka 메시지 소비
4. Processor Service: MySQL Upsert + Redis Geo/Cache 갱신

### 조회 흐름
1. Client: GET /api/v1/facilities/search?lat=...&lng=...&radius=...
2. API Service: Redis GEOSEARCH로 반경 내 시설 ID 조회
3. API Service: Redis 캐시에서 시설 정보 조회 (Cache Aside)
4. API Service: Cache Miss 시 MySQL 조회 → 캐시 저장
5. Client: 거리순 정렬된 결과 반환

---

## 4. 향후 확장 가능 컨텍스트

### Reservation Context
- 주차장 예약
- 충전 예약
- 결제 연동

### Navigation Context
- 경로 안내
- 실시간 교통
- 도착 예정 시간

### Analytics Context
- 이용 통계
- 트렌드 분석
- 피크 시간대

### Notification Context
- 가용성 알림
- 충전 완료 알림
- 요금 변동 알림

### User Preference Context
- 즐겨찾기
- 검색 히스토리
- 맞춤 추천
