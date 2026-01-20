# Redis 캐싱 가이드

## 개요

주차장 검색 API에 Redis 캐싱을 적용하여 응답 속도를 개선합니다.

---

## 1. 캐싱 전략

### 1.1 Look-aside (Cache-aside) 패턴

```
┌─────────┐     ┌─────────┐     ┌─────────────────┐
│  Client │────▶│   API   │────▶│  Redis (Cache)  │
└─────────┘     └────┬────┘     └────────┬────────┘
                     │                   │
                     │ cache miss        │ cache hit
                     ▼                   │
              ┌─────────────────┐        │
              │  ElasticSearch  │        │
              └─────────────────┘        │
                     │                   │
                     └───────────────────┘
                         결과 반환
```

### 1.2 캐싱 대상

| 데이터 유형 | TTL | 캐싱 여부 |
|------------|-----|----------|
| 검색 결과 | 1~5분 | O |
| 주차장 상세 정보 | 30분 | O |
| 실시간 가용성 | 30초 | O (짧은 TTL) |
| 요금 정보 | 1시간 | O |

---

## 2. Redis 설정

### 2.1 의존성 추가

```gradle
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
}
```

### 2.2 application.yml

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 3000ms

  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5분 (기본 TTL)
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "parking:"
```

### 2.3 Redis Config

```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettucePoolingClientConfiguration poolConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(genericObjectPoolConfig())
            .commandTimeout(Duration.ofMillis(3000))
            .build();

        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName("localhost");
        serverConfig.setPort(6379);

        return new LettuceConnectionFactory(serverConfig, poolConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer
        template.setKeySerializer(new StringRedisSerializer());

        // Value Serializer (JSON)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 캐시별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "parkingSearch", defaultConfig.entryTtl(Duration.ofMinutes(1)),
            "parkingDetail", defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "parkingAvailability", defaultConfig.entryTtl(Duration.ofSeconds(30))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    private GenericObjectPoolConfig<?> genericObjectPoolConfig() {
        GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(20);
        config.setMaxIdle(10);
        config.setMinIdle(5);
        config.setMaxWait(Duration.ofMillis(3000));
        return config;
    }
}
```

---

## 3. 캐시 키 전략

### 3.1 검색 결과 캐시 키

```java
public class CacheKeyGenerator {

    /**
     * 검색 캐시 키 생성
     * 형식: parking:search:{geohash}:{radius}:{filters}
     */
    public static String generateSearchKey(ParkingSearchRequest request) {
        // Geohash 5자리 (약 5km 정밀도)
        String geohash = GeoHash.encodeHash(request.getLat(), request.getLng(), 5);

        // 반경 반올림 (0.5km 단위)
        double roundedRadius = Math.round(request.getRadius() * 2) / 2.0;

        // 필터 해시
        String filterHash = generateFilterHash(request);

        return String.format("parking:search:%s:%.1f:%s:%d:%d",
            geohash,
            roundedRadius,
            filterHash,
            request.getPage(),
            request.getSize()
        );
    }

    private static String generateFilterHash(ParkingSearchRequest request) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(request.getAvailable())) sb.append("A");
        if (Boolean.TRUE.equals(request.getFree())) sb.append("F");
        return sb.length() > 0 ? sb.toString() : "N";
    }
}
```

### 3.2 Geohash 정밀도

| 정밀도 | 셀 크기 | 용도 |
|--------|--------|------|
| 4 | ~20km | 광역 검색 |
| 5 | ~5km | 일반 검색 (권장) |
| 6 | ~1km | 정밀 검색 |
| 7 | ~150m | 상세 검색 |

---

## 4. Service 구현

### 4.1 캐시 적용 Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSearchService {

    private final ParkingSearchRepository parkingSearchRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(1);

    public List<ParkingResponse> search(ParkingSearchRequest request) {
        String cacheKey = CacheKeyGenerator.generateSearchKey(request);

        // 1. 캐시 조회
        List<ParkingResponse> cached = getFromCache(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for key: {}", cacheKey);
            return cached;
        }

        log.debug("Cache miss for key: {}", cacheKey);

        // 2. ES 검색
        List<ParkingResponse> result = searchFromElasticsearch(request);

        // 3. 캐시 저장 (비동기)
        saveToCache(cacheKey, result);

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ParkingResponse> getFromCache(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.convertValue(cached,
                    new TypeReference<List<ParkingResponse>>() {});
            }
        } catch (Exception e) {
            log.warn("Cache read failed: {}", e.getMessage());
        }
        return null;
    }

    @Async
    public void saveToCache(String key, List<ParkingResponse> result) {
        try {
            if (!result.isEmpty()) {
                redisTemplate.opsForValue().set(key, result, SEARCH_CACHE_TTL);
            }
        } catch (Exception e) {
            log.warn("Cache write failed: {}", e.getMessage());
        }
    }

    private List<ParkingResponse> searchFromElasticsearch(ParkingSearchRequest request) {
        // 기존 ES 검색 로직
        SearchHits<ParkingDocument> hits = parkingSearchRepository.search(
            request.getLat(),
            request.getLng(),
            request.getRadius(),
            request.getAvailable(),
            request.getFree(),
            PageRequest.of(request.getPage(), request.getSize())
        );

        return hits.stream()
            .map(this::toResponse)
            .toList();
    }
}
```

### 4.2 어노테이션 기반 캐싱

```java
@Service
@RequiredArgsConstructor
public class ParkingSearchService {

    @Cacheable(
        value = "parkingSearch",
        key = "T(com.example.api.util.CacheKeyGenerator).generateSearchKey(#request)",
        unless = "#result.isEmpty()"
    )
    public List<ParkingResponse> search(ParkingSearchRequest request) {
        return searchFromElasticsearch(request);
    }

    @CacheEvict(value = "parkingSearch", allEntries = true)
    public void evictSearchCache() {
        log.info("Search cache evicted");
    }

    @Cacheable(value = "parkingDetail", key = "#id")
    public ParkingResponse findById(Long id) {
        // ...
    }
}
```

---

## 5. Redis Geo 명령어 활용

### 5.1 주차장 위치 저장

```java
@Service
@RequiredArgsConstructor
public class ParkingGeoService {

    private final StringRedisTemplate redisTemplate;
    private static final String GEO_KEY = "parking:locations";

    /**
     * 주차장 위치 저장
     */
    public void addLocation(Long parkingId, double lat, double lng) {
        redisTemplate.opsForGeo().add(
            GEO_KEY,
            new Point(lng, lat),  // Redis는 lng, lat 순서
            parkingId.toString()
        );
    }

    /**
     * 반경 내 주차장 검색
     */
    public List<Long> searchByRadius(double lat, double lng, double radiusKm) {
        Circle circle = new Circle(
            new Point(lng, lat),
            new Distance(radiusKm, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
            redisTemplate.opsForGeo().radius(
                GEO_KEY,
                circle,
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .includeDistance()
                    .sortAscending()
                    .limit(100)
            );

        return results.getContent().stream()
            .map(r -> Long.parseLong(r.getContent().getName()))
            .toList();
    }

    /**
     * 두 주차장 간 거리 계산
     */
    public Double getDistance(Long id1, Long id2) {
        Distance distance = redisTemplate.opsForGeo().distance(
            GEO_KEY,
            id1.toString(),
            id2.toString(),
            Metrics.KILOMETERS
        );
        return distance != null ? distance.getValue() : null;
    }
}
```

### 5.2 Geo + 캐싱 조합

```java
@Service
@RequiredArgsConstructor
public class ParkingSearchService {

    private final ParkingGeoService geoService;
    private final RedisTemplate<String, ParkingResponse> cacheTemplate;

    public List<ParkingResponse> searchWithGeo(double lat, double lng, double radiusKm) {
        // 1. Redis GEO로 ID 목록 조회 (매우 빠름)
        List<Long> parkingIds = geoService.searchByRadius(lat, lng, radiusKm);

        if (parkingIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. ID로 상세 정보 조회 (캐시 + DB)
        List<String> cacheKeys = parkingIds.stream()
            .map(id -> "parking:detail:" + id)
            .toList();

        // Multi-get으로 캐시 일괄 조회
        List<ParkingResponse> cached = cacheTemplate.opsForValue()
            .multiGet(cacheKeys);

        // 3. 캐시 미스인 항목은 DB에서 조회
        List<Long> missedIds = new ArrayList<>();
        List<ParkingResponse> result = new ArrayList<>();

        for (int i = 0; i < parkingIds.size(); i++) {
            if (cached.get(i) != null) {
                result.add(cached.get(i));
            } else {
                missedIds.add(parkingIds.get(i));
            }
        }

        if (!missedIds.isEmpty()) {
            List<ParkingResponse> fromDb = loadFromDatabase(missedIds);
            result.addAll(fromDb);
            // 캐시에 저장
            cacheToRedis(fromDb);
        }

        return result;
    }
}
```

---

## 6. 캐시 무효화 전략

### 6.1 이벤트 기반 무효화

```java
@Component
@RequiredArgsConstructor
public class CacheInvalidationListener {

    private final RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = "facility-events")
    public void onFacilityEvent(FacilityEvent event) {
        if (event.getType() == FacilityType.PARKING) {
            // 해당 지역 검색 캐시 무효화
            String geohash = GeoHash.encodeHash(
                event.getLatitude(),
                event.getLongitude(),
                5
            );

            // 패턴 매칭으로 관련 캐시 삭제
            Set<String> keys = redisTemplate.keys("parking:search:" + geohash + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            // 상세 정보 캐시도 무효화
            redisTemplate.delete("parking:detail:" + event.getId());
        }
    }
}
```

### 6.2 TTL 기반 자동 만료

| 캐시 유형 | TTL | 이유 |
|----------|-----|------|
| 검색 결과 | 1분 | 가용성 변화 반영 |
| 상세 정보 | 30분 | 기본 정보는 변경 적음 |
| 요금 정보 | 1시간 | 거의 변경 없음 |

---

## 7. 에러 핸들링

### 7.1 Circuit Breaker 적용

```java
@Service
@RequiredArgsConstructor
public class ResilientCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CircuitBreaker circuitBreaker;

    public <T> Optional<T> getWithFallback(String key, Class<T> type) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return Optional.of(type.cast(value));
                }
            } catch (Exception e) {
                log.warn("Redis read failed, key={}: {}", key, e.getMessage());
            }
            return Optional.empty();
        });
    }

    public void setWithFallback(String key, Object value, Duration ttl) {
        circuitBreaker.executeRunnable(() -> {
            try {
                redisTemplate.opsForValue().set(key, value, ttl);
            } catch (Exception e) {
                log.warn("Redis write failed, key={}: {}", key, e.getMessage());
            }
        });
    }
}
```

### 7.2 Fallback 전략

```java
public List<ParkingResponse> search(ParkingSearchRequest request) {
    try {
        // 1. 캐시 시도
        return cacheService.getWithFallback(cacheKey, listType)
            .orElseGet(() -> {
                // 2. Cache miss -> ES 검색
                List<ParkingResponse> result = searchFromElasticsearch(request);
                cacheService.setWithFallback(cacheKey, result, ttl);
                return result;
            });
    } catch (Exception e) {
        // 3. 모든 실패 시 ES 직접 검색
        log.error("Cache layer failed, falling back to ES", e);
        return searchFromElasticsearch(request);
    }
}
```

---

## 8. 체크리스트

- [ ] Redis 의존성 및 설정 추가
- [ ] RedisConfig 클래스 생성
- [ ] 캐시 키 생성 유틸리티 구현
- [ ] Service에 캐싱 로직 적용
- [ ] Redis GEO 명령어 활용 (선택)
- [ ] 캐시 무효화 이벤트 리스너 구현
- [ ] Circuit Breaker 적용
- [ ] JMeter로 캐시 히트율 및 성능 측정

---

## 9. 참고 자료

- [Spring Data Redis Documentation](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Redis GEO Commands](https://redis.io/docs/latest/commands/?group=geo)
- [Geohash.org](http://geohash.org/)
