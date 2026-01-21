package com.example.collection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Redis 기반 변경 감지 서비스
 *
 * 공공 API는 updatedAt을 제공하지 않으므로 해시 기반으로 변경 감지:
 * - 시설정보/운영정보: 필드 해시 비교
 * - 실시간정보: 값 직접 비교
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeDetectionService {

    private final StringRedisTemplate redisTemplate;

    private static final String FACILITY_HASH_PREFIX = "hash:facility:";
    private static final String OPERATION_HASH_PREFIX = "hash:operation:";
    private static final String AVAILABILITY_PREFIX = "avail:";
    private static final Duration HASH_TTL = Duration.ofDays(7);

    /**
     * 시설정보 변경 여부 확인
     * @return true if changed or new, false if unchanged
     */
    public boolean isFacilityInfoChanged(String externalId, String name, String address,
                                          double lat, double lng, int totalCount) {
        String key = FACILITY_HASH_PREFIX + externalId;
        String currentHash = computeHash(name, address, lat, lng, totalCount);

        String previousHash = redisTemplate.opsForValue().get(key);

        if (previousHash == null) {
            // 신규 데이터
            redisTemplate.opsForValue().set(key, currentHash, HASH_TTL);
            return true;
        }

        if (!currentHash.equals(previousHash)) {
            // 변경됨
            redisTemplate.opsForValue().set(key, currentHash, HASH_TTL);
            return true;
        }

        return false;
    }

    /**
     * 운영정보 변경 여부 확인
     * @return true if changed, false if unchanged
     */
    public boolean isOperationInfoChanged(String externalId, String operationInfo) {
        String key = OPERATION_HASH_PREFIX + externalId;
        String currentHash = computeHash(operationInfo);

        String previousHash = redisTemplate.opsForValue().get(key);

        if (previousHash == null) {
            redisTemplate.opsForValue().set(key, currentHash, HASH_TTL);
            return true;
        }

        if (!currentHash.equals(previousHash)) {
            redisTemplate.opsForValue().set(key, currentHash, HASH_TTL);
            return true;
        }

        return false;
    }

    /**
     * 가용성 변경 여부 확인 (값 직접 비교)
     * @return true if changed, false if unchanged
     */
    public boolean isAvailabilityChanged(String externalId, int availableCount) {
        String key = AVAILABILITY_PREFIX + externalId;
        String currentValue = String.valueOf(availableCount);

        String previousValue = redisTemplate.opsForValue().get(key);

        if (previousValue == null) {
            redisTemplate.opsForValue().set(key, currentValue, Duration.ofMinutes(10));
            return true;
        }

        if (!currentValue.equals(previousValue)) {
            redisTemplate.opsForValue().set(key, currentValue, Duration.ofMinutes(10));
            return true;
        }

        return false;
    }

    /**
     * 시설 존재 여부 확인 (삭제 감지용)
     */
    public boolean facilityExists(String externalId) {
        String key = FACILITY_HASH_PREFIX + externalId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 시설 삭제 처리
     */
    public void markFacilityDeleted(String externalId) {
        redisTemplate.delete(FACILITY_HASH_PREFIX + externalId);
        redisTemplate.delete(OPERATION_HASH_PREFIX + externalId);
        redisTemplate.delete(AVAILABILITY_PREFIX + externalId);
    }

    private String computeHash(Object... values) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder sb = new StringBuilder();
            for (Object value : values) {
                sb.append(value != null ? value.toString() : "null").append("|");
            }
            byte[] hashBytes = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
