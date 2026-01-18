# 주차장/충전소 도메인 모델

## 프로젝트 목적
DDD 방법론 기반 도메인 설계

## 구조
```
parking-charging/
├── common/          # 도메인 모델 (Entity, Value Object, Event)
└── docs/            # DDD 설계 문서
```

## 기술 스택
- Java 17
- JPA (Jakarta Persistence API)
- Lombok

## DDD 설계 문서
- `docs/01_UBIQUITOUS_LANGUAGE.md` - 유비쿼터스 언어, 용어 사전
- `docs/02_STRATEGIC_DESIGN.md` - 바운디드 컨텍스트, 컨텍스트 맵
- `docs/03_TACTICAL_DESIGN.md` - 애그리거트, 엔티티, 값 객체
- `docs/04_EVENT_FLOW.md` - 이벤트 플로우, 읽기 모델

## 도메인 모델
- `Facility` - 시설 엔티티 (Aggregate Root)
- `Location` - 위치 좌표 (Value Object)
- `Availability` - 가용성/점유율 (Value Object)
- `ParkingFee` - 주차 요금 계산 (Value Object)
- `FacilityEvent` - Kafka 이벤트 DTO
