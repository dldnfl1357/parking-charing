# 주차장 검색 API 성능 최적화 로드맵

## 개요

주차장 검색 API의 성능을 단계별로 최적화하는 가이드입니다.

### 대상 API

| 엔드포인트 | 설명 | 저장소 |
|-----------|------|--------|
| `GET /api/v1/parkings/search` | 주차장 검색 (기본) | ElasticSearch |
| `GET /api/v1/parkings/search/mysql` | 주차장 검색 (벤치마크) | MySQL |

### 검색 파라미터

```
lat      : 위도 (필수)
lng      : 경도 (필수)
radius   : 검색 반경 km (기본값: 5.0)
available: 주차 가능 여부 필터
free     : 무료 주차장 필터
page     : 페이지 번호 (기본값: 0)
size     : 페이지 크기 (기본값: 20)
```

---

## 최적화 단계

```
┌─────────────────────────────────────────────────────────────────┐
│                    성능 최적화 로드맵                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1단계: MySQL 최적화                                             │
│  ├── 공간 인덱스 (Spatial Index)                                 │
│  ├── 쿼리 최적화 (ST_Distance_Sphere)                            │
│  └── Connection Pool 튜닝                                       │
│           │                                                     │
│           ▼                                                     │
│  2단계: ElasticSearch 추가 ✅ (완료)                              │
│  ├── Geo Distance Query                                         │
│  └── Filter Context 활용                                        │
│           │                                                     │
│           ▼                                                     │
│  3단계: ElasticSearch 최적화                                     │
│  ├── Routing 설정                                               │
│  ├── Shard 최적화                                               │
│  └── Geo-Hash 사전 집계                                         │
│           │                                                     │
│           ▼                                                     │
│  4단계: Redis 캐싱 추가                                          │
│  ├── Look-aside Cache                                           │
│  ├── Redis Geo 명령어                                           │
│  └── 캐시 키 전략                                                │
│           │                                                     │
│           ▼                                                     │
│  5단계: Redis 최적화                                             │
│  ├── Redis Cluster                                              │
│  ├── Pipeline / Lua Script                                      │
│  └── Hot Key 분산                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 문서 목록

| 문서 | 설명 |
|------|------|
| [01_MYSQL_OPTIMIZATION.md](./01_MYSQL_OPTIMIZATION.md) | MySQL 인덱스 및 쿼리 최적화 |
| [02_ELASTICSEARCH_OPTIMIZATION.md](./02_ELASTICSEARCH_OPTIMIZATION.md) | ES 매핑, 샤드, Geo 쿼리 최적화 |
| [03_REDIS_CACHING.md](./03_REDIS_CACHING.md) | Redis 캐싱 전략 및 구현 |
| [04_JMETER_TEST.md](./04_JMETER_TEST.md) | JMeter 성능 테스트 가이드 |

---

## 기대 성능 목표

| 지표 | 현재 (예상) | 목표 |
|------|------------|------|
| TPS | 100 | 1,000+ |
| P95 응답시간 | 500ms | 50ms |
| P99 응답시간 | 1,000ms | 100ms |
| 에러율 | 1% | 0.01% |

---

## 아키텍처 변화

### AS-IS (MySQL Only)
```
Client → API Server → MySQL
                         └── Haversine 거리 계산
```

### TO-BE (Multi-tier Cache)
```
Client → API Server → Redis Cache (L1)
                         │ (cache miss)
                         ▼
                      ElasticSearch (L2)
                         │ (index miss)
                         ▼
                      MySQL (Source of Truth)
```

---

## 관련 파일

```
service-api/
├── controller/
│   └── ParkingSearchController.java
├── service/
│   └── ParkingSearchService.java
├── repository/
│   ├── ParkingSearchRepository.java  (ES)
│   └── FacilityRepository.java       (MySQL)
└── document/
    └── ParkingDocument.java

service-common/
└── util/
    └── GeoUtils.java
```
