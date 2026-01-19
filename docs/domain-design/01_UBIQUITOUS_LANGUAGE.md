# 유비쿼터스 언어 (Ubiquitous Language)

모빌리티 도메인에서 개발자, 기획자, 도메인 전문가가 공통으로 사용하는 용어 정의.

---

## 핵심 용어

| 한글 | 영문 | 정의 |
|------|------|------|
| 시설 | Facility | 주차장 또는 충전소를 총칭하는 상위 개념 |
| 주차장 | ParkingLot | 차량을 주차할 수 있는 시설 |
| 충전소 | ChargingStation | 전기차를 충전할 수 있는 시설 |
| 충전기 | Charger | 충전소 내 개별 충전 장치 |

## 위치/주소

| 한글 | 영문 | 정의 |
|------|------|------|
| 위치 | Location | 시설의 지리적 좌표 (위도, 경도) |
| 주소 | Address | 시설의 도로명/지번 주소 |
| 반경 검색 | RadiusSearch | 특정 좌표 기준 반경 내 시설 검색 |

## 가용성

| 한글 | 영문 | 정의 |
|------|------|------|
| 가용성 | Availability | 현재 이용 가능한 주차면/충전기 수 |
| 용량 | Capacity | 전체 주차면/충전기 수 |
| 점유율 | OccupancyRate | (용량 - 가용성) / 용량 × 100 |
| 실시간 상태 | RealTimeStatus | 시설의 현재 운영/가용 상태 |

## 운영 정보

| 한글 | 영문 | 정의 |
|------|------|------|
| 요금 정책 | PricingPolicy | 시설 이용 요금 체계 |
| 운영 시간 | OperatingHours | 시설 운영 시간대 |
| 충전 타입 | ChargerType | 급속/완속 충전 구분 |
| 충전 출력 | ChargerOutput | 충전기 출력 (kW) |

## 데이터 처리

| 한글 | 영문 | 정의 |
|------|------|------|
| 외부 식별자 | ExternalId | 공공 API에서 부여한 고유 ID |
| 데이터 수집 | DataCollection | 외부 API로부터 정보를 가져오는 행위 |
| 데이터 동기화 | DataSync | 수집된 데이터를 시스템에 반영하는 행위 |

---

## 용어 사전 (Glossary)

| 용어 | 정의 | 사용 컨텍스트 |
|------|------|---------------|
| Facility | 주차장 또는 충전소를 총칭 | 전체 |
| ParkingLot | 차량 주차 시설 | Processing, Search |
| ChargingStation | 전기차 충전 시설 | Processing, Search |
| Charger | 충전소 내 개별 충전기 | Processing |
| Location | 위도/경도 좌표 | 전체 |
| Availability | 현재 가용 수량 상태 | Processing, Search |
| ExternalId | 외부 공공 API 고유 식별자 | Collection, Processing |
| GeoIndex | Redis Geo 기반 공간 인덱스 | Search |
| DataSync | 외부 → 내부 데이터 반영 | Collection, Processing |
