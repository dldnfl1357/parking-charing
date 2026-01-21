package com.example.api.service;

import com.example.api.document.ParkingDocument;
import com.example.api.dto.ParkingResponse;
import com.example.api.dto.ParkingSearchRequest;
import com.example.api.repository.FacilityRepository;
import com.example.api.repository.ParkingSearchRepository;
import com.example.api.util.CacheKeyGenerator;
import com.example.common.domain.FacilityType;
import com.example.common.domain.entity.Facility;
import com.example.common.util.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * 주차장 검색 서비스 (MySQL / ElasticSearch / Redis 캐싱)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSearchService {

    private final ParkingSearchRepository parkingSearchRepository;
    private final FacilityRepository facilityRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_PREFIX = "parking:search:";
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(1);

    // 캐시 통계 (디버깅용)
    private final java.util.concurrent.atomic.AtomicLong cacheHitCount = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong cacheMissCount = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong cacheErrorCount = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * 위치 기반 주차장 검색 (ES + Redis 캐싱)
     */
    public List<ParkingResponse> search(ParkingSearchRequest request) {
        String cacheKey = CACHE_PREFIX + CacheKeyGenerator.generateSearchKey(request);

        // 1. 캐시 조회
        long redisStart = System.currentTimeMillis();
        List<ParkingResponse> cached = getFromCache(cacheKey);
        long redisTime = System.currentTimeMillis() - redisStart;

        if (cached != null) {
            long hitCount = cacheHitCount.incrementAndGet();
            if (hitCount % 1000 == 0) {
                logCacheStats();
            }
            log.debug("[Cache HIT] key={}, redisTime={}ms", cacheKey, redisTime);
            return cached;
        }

        long missCount = cacheMissCount.incrementAndGet();
        log.debug("[Cache MISS] key={}, redisCheckTime={}ms", cacheKey, redisTime);

        // 2. ES 검색
        long esStart = System.currentTimeMillis();
        List<ParkingResponse> result = searchFromElasticsearch(request);
        long esTime = System.currentTimeMillis() - esStart;

        // 3. 캐시 저장
        long saveStart = System.currentTimeMillis();
        saveToCache(cacheKey, result);
        long saveTime = System.currentTimeMillis() - saveStart;

        if (missCount % 100 == 0) {
            log.info("[Cache MISS #{}} redisCheck={}ms, esQuery={}ms, redisSave={}ms",
                    missCount, redisTime, esTime, saveTime);
        }

        return result;
    }

    private void logCacheStats() {
        long hits = cacheHitCount.get();
        long misses = cacheMissCount.get();
        long errors = cacheErrorCount.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (hits * 100.0 / total) : 0;
        log.info("[Cache Stats] hits={}, misses={}, errors={}, hitRate={}%",
                hits, misses, errors, String.format("%.1f", hitRate));
    }

    /**
     * 위치 기반 주차장 검색 (ES, 캐시 우회)
     */
    public List<ParkingResponse> searchNoCache(ParkingSearchRequest request) {
        return searchFromElasticsearch(request);
    }

    private List<ParkingResponse> searchFromElasticsearch(ParkingSearchRequest request) {
        SearchHits<ParkingDocument> hits = parkingSearchRepository.search(
                request.getLat(),
                request.getLng(),
                request.getRadius(),
                request.getAvailable(),
                request.getFree(),
                request.getPage(),
                request.getSize()
        );

        return hits.getSearchHits().stream()
                .map(this::toResponse)
                .toList();
    }

    private List<ParkingResponse> getFromCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<ParkingResponse>>() {});
            }
        } catch (Exception e) {
            cacheErrorCount.incrementAndGet();
            log.warn("[Cache] Read failed: key={}, error={}", key, e.getMessage());
        }
        return null;
    }

    private void saveToCache(String key, List<ParkingResponse> result) {
        try {
            if (result != null && !result.isEmpty()) {
                String json = objectMapper.writeValueAsString(result);
                stringRedisTemplate.opsForValue().set(key, json, SEARCH_CACHE_TTL);
                log.debug("[Cache] Saved: key={}, size={}", key, result.size());
            }
        } catch (Exception e) {
            cacheErrorCount.incrementAndGet();
            log.warn("[Cache] Write failed: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 검색 캐시 무효화
     */
    public void evictSearchCache() {
        try {
            var keys = stringRedisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.info("[Cache] Evicted {} keys", keys.size());
            }
        } catch (Exception e) {
            log.warn("[Cache] Eviction failed: {}", e.getMessage());
        }
    }

    private ParkingResponse toResponse(SearchHit<ParkingDocument> hit) {
        ParkingDocument doc = hit.getContent();

        // 거리 정보 추출 (geo_distance 정렬 시)
        Double distance = null;
        List<Object> sortValues = hit.getSortValues();
        if (!sortValues.isEmpty() && sortValues.get(0) instanceof Number) {
            distance = ((Number) sortValues.get(0)).doubleValue();
        }

        // extraInfo 파싱
        JsonNode extraInfo = parseExtraInfo(doc.getExtraInfo());

        return ParkingResponse.builder()
                .id(parseIdSafely(doc.getId()))
                .externalId(doc.getExternalId())
                .name(doc.getName())
                .address(doc.getAddress())
                .latitude(doc.getLocation() != null ? doc.getLocation().getLat() : 0)
                .longitude(doc.getLocation() != null ? doc.getLocation().getLon() : 0)
                .totalCount(doc.getTotalCount())
                .availableCount(doc.getAvailableCount())
                .distance(distance != null ? Math.round(distance * 100.0) / 100.0 : null)
                .baseFee(extractBaseFee(extraInfo))
                .addFee(extractAddFee(extraInfo))
                .dayMaxCrg(extractDayMaxCrg(extraInfo))
                .weekdayOperTime(extractStringField(extraInfo, "weekdayOperTime"))
                .source(extractStringField(extraInfo, "source"))
                .build();
    }

    /**
     * 위치 기반 주차장 검색 (MySQL)
     */
    public List<ParkingResponse> searchByMysql(ParkingSearchRequest request) {
        log.info("[MySQL] Parking search: lat={}, lng={}, radius={}km",
                request.getLat(), request.getLng(), request.getRadius());

        long startTime = System.currentTimeMillis();

        // Bounding box 계산 (성능 최적화)
        double[] bbox = GeoUtils.getBoundingBox(
                request.getLat(), request.getLng(), request.getRadius());

        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize());

        List<Facility> facilities;
        if (Boolean.TRUE.equals(request.getAvailable())) {
            facilities = facilityRepository.findAvailableByLocationWithinRadius(
                    request.getLat(), request.getLng(), request.getRadius(),
                    FacilityType.PARKING.name(),
                    bbox[0], bbox[1], bbox[2], bbox[3],
                    pageable);
        } else {
            facilities = facilityRepository.findByLocationWithinRadius(
                    request.getLat(), request.getLng(), request.getRadius(),
                    FacilityType.PARKING.name(),
                    bbox[0], bbox[1], bbox[2], bbox[3],
                    pageable);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[MySQL] Search completed: {} results in {}ms", facilities.size(), elapsed);

        return facilities.stream()
                .map(f -> toResponseFromFacility(f, request.getLat(), request.getLng()))
                .toList();
    }

    private ParkingResponse toResponseFromFacility(Facility facility, double lat, double lng) {
        double distance = GeoUtils.calculateDistance(
                lat, lng,
                facility.getLocation().getLatitude(),
                facility.getLocation().getLongitude());

        // extraInfo 파싱
        JsonNode extraInfo = parseExtraInfo(facility.getExtraInfo());

        return ParkingResponse.builder()
                .id(facility.getId())
                .externalId(facility.getExternalId())
                .name(facility.getName())
                .address(facility.getAddress())
                .latitude(facility.getLocation().getLatitude())
                .longitude(facility.getLocation().getLongitude())
                .totalCount(facility.getAvailability().getTotalCount())
                .availableCount(facility.getAvailability().getAvailableCount())
                .distance(Math.round(distance * 100.0) / 100.0)
                .baseFee(extractBaseFee(extraInfo))
                .addFee(extractAddFee(extraInfo))
                .dayMaxCrg(extractDayMaxCrg(extraInfo))
                .weekdayOperTime(extractStringField(extraInfo, "weekdayOperTime"))
                .source(extractStringField(extraInfo, "source"))
                .build();
    }

    // === extraInfo 파싱 헬퍼 메서드 ===

    private JsonNode parseExtraInfo(String extraInfoJson) {
        if (extraInfoJson == null || extraInfoJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(extraInfoJson);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse extraInfo: {}", extraInfoJson, e);
            return null;
        }
    }

    private String extractBaseFee(JsonNode extraInfo) {
        if (extraInfo == null) {
            return null;
        }

        // TS API 형식: "baseFee": "30분 1000원"
        if (extraInfo.has("baseFee") && extraInfo.get("baseFee").isTextual()) {
            return extraInfo.get("baseFee").asText();
        }

        // 공공데이터 형식: baseFee (숫자) + baseMinutes
        if (extraInfo.has("baseFee") && extraInfo.get("baseFee").isNumber()) {
            int baseFee = extraInfo.get("baseFee").asInt();
            int baseMinutes = extraInfo.has("baseMinutes") ? extraInfo.get("baseMinutes").asInt() : 0;
            if (baseFee > 0 && baseMinutes > 0) {
                return baseMinutes + "분 " + formatCurrency(baseFee);
            } else if (baseFee == 0) {
                return "무료";
            }
        }

        return null;
    }

    private String extractAddFee(JsonNode extraInfo) {
        if (extraInfo == null) {
            return null;
        }

        // TS API 형식: "addFee": "10분당 500원"
        if (extraInfo.has("addFee") && extraInfo.get("addFee").isTextual()) {
            return extraInfo.get("addFee").asText();
        }

        // 공공데이터 형식: unitFee (숫자) + unitMinutes
        if (extraInfo.has("unitFee") && extraInfo.get("unitFee").isNumber()) {
            int unitFee = extraInfo.get("unitFee").asInt();
            int unitMinutes = extraInfo.has("unitMinutes") ? extraInfo.get("unitMinutes").asInt() : 0;
            if (unitFee > 0 && unitMinutes > 0) {
                return unitMinutes + "분당 " + formatCurrency(unitFee);
            }
        }

        return null;
    }

    private String extractDayMaxCrg(JsonNode extraInfo) {
        if (extraInfo == null) {
            return null;
        }

        // TS API 형식: "dayMaxCrg": 15000 (숫자)
        if (extraInfo.has("dayMaxCrg") && extraInfo.get("dayMaxCrg").isNumber()) {
            int dayMaxCrg = extraInfo.get("dayMaxCrg").asInt();
            if (dayMaxCrg > 0) {
                return formatCurrency(dayMaxCrg);
            }
        }

        // 공공데이터 형식: "dailyMaxFee"
        if (extraInfo.has("dailyMaxFee") && extraInfo.get("dailyMaxFee").isNumber()) {
            int dailyMaxFee = extraInfo.get("dailyMaxFee").asInt();
            if (dailyMaxFee > 0) {
                return formatCurrency(dailyMaxFee);
            }
        }

        return null;
    }

    private String extractStringField(JsonNode extraInfo, String fieldName) {
        if (extraInfo == null || !extraInfo.has(fieldName)) {
            return null;
        }
        JsonNode field = extraInfo.get(fieldName);
        return field.isTextual() ? field.asText() : null;
    }

    private String formatCurrency(int amount) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원";
    }

    private Long parseIdSafely(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            // 숫자가 아닌 ID (예: TEST_xxx)는 null 반환
            return null;
        }
    }
}
