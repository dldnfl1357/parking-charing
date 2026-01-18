# 주차장/충전소 통합 플랫폼 - 도메인 설계

DDD(Domain-Driven Design) 방법론을 적용한 주차장/충전소 도메인 모델링 프로젝트

## 도메인 개요

실시간 주차장 및 전기차 충전소 정보를 통합 조회하는 플랫폼의 핵심 도메인 설계

## 프로젝트 구조

```
├── common/                     # 도메인 모듈
│   └── src/main/java/
│       └── com/example/common/
│           ├── domain/
│           │   ├── entity/     # Aggregate Root
│           │   └── vo/         # Value Objects
│           ├── event/          # Domain Events
│           └── util/           # 유틸리티
│
└── docs/                       # DDD 설계 문서
    ├── 01_UBIQUITOUS_LANGUAGE.md
    ├── 02_STRATEGIC_DESIGN.md
    ├── 03_TACTICAL_DESIGN.md
    └── 04_EVENT_FLOW.md
```

## 핵심 도메인 모델

| 구분 | 클래스 | 설명 |
|------|--------|------|
| Entity | Facility | 시설 (주차장/충전소) Aggregate Root |
| Value Object | Location | 위치 좌표 + 거리 계산 |
| Value Object | Availability | 가용성 + 점유율/혼잡도 |
| Value Object | ParkingFee | 주차 요금 계산 로직 |

## 기술 스택

- Java 17
- JPA (Jakarta Persistence)
- Lombok
