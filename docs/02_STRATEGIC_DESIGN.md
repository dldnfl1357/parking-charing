# 전략적 설계 (Strategic Design)

---

## 1. 도메인 분류

### 핵심 도메인 (Core Domain)
비즈니스 경쟁력의 핵심이 되는 도메인.

**시설 검색 도메인**
- 위치 기반 시설 검색
- 실시간 가용성 조회
- 필터링 (타입, 요금, 운영시간 등)

### 지원 도메인 (Supporting Domain)
핵심 도메인을 지원하는 도메인.

**데이터 수집 도메인**
- 외부 API 연동
- 데이터 변환
- 스케줄링

**시설 관리 도메인**
- 시설 정보 저장
- 상태 갱신
- 이력 관리

### 일반 도메인 (Generic Domain)
범용적으로 사용되는 도메인.

**위치 서비스**
- 좌표 계산
- 거리 계산
- 주소 변환

**캐싱 서비스**
- 조회 결과 캐싱
- Geo 인덱싱
- TTL 관리

---

## 2. 바운디드 컨텍스트 (Bounded Context)

### Collection Context
- 책임: 외부 API 데이터 수집 및 이벤트 발행
- 핵심 모델: ParkingData, ChargingData, CollectionSchedule
- 언어: 수집, 변환, API 호출

### Processing Context
- 책임: 이벤트 처리, 데이터 저장/갱신
- 핵심 모델: Facility, ParkingLot, ChargingStation, Availability
- 언어: 시설, 저장, 갱신, 동기화

### Search Context
- 책임: 위치 기반 검색, 조회 API 제공
- 핵심 모델: FacilitySummary, SearchCriteria, SearchResult
- 언어: 검색, 조회, 필터, 정렬

---

## 3. 컨텍스트 맵 (Context Map)

### 관계 구조

External Public API → Collection Context → Processing Context → Search Context

### 관계 유형

**Conformist: Collection ← External API**
- 외부 API 스키마를 그대로 수용
- ACL(변환 계층)으로 내부 도메인 격리

**Published Language: Collection → Processing**
- FacilityEvent를 통한 표준화된 도메인 이벤트 교환
- Kafka 메시지 기반 비동기 통신

**Shared Kernel: Processing ↔ Search**
- Facility 핵심 모델 공유 (common 모듈)
- 동일한 엔티티/값 객체 사용

---

## 4. Anti-Corruption Layer (ACL)

외부 공공 API 스키마 변경으로부터 내부 도메인 보호.

### 변환 흐름
1. External API Client: 외부 API 호출, 응답 수신
2. ACL (Translator): 외부 DTO → 내부 도메인 객체 변환
3. Domain Model: 내부 비즈니스 로직 처리

### 변환 예시
- 외부: 주차장코드, 주차장명, 위도, 경도
- 내부: externalId, name, location.latitude, location.longitude
