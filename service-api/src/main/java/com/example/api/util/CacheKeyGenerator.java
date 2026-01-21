package com.example.api.util;

import com.example.api.dto.ParkingSearchRequest;

/**
 * Redis 캐시 키 생성 유틸리티
 */
public final class CacheKeyGenerator {

    private CacheKeyGenerator() {
        // 유틸리티 클래스
    }

    /**
     * 주차장 검색 캐시 키 생성
     * 형식: {lat}:{lng}:{radius}:{filters}:{page}:{size}
     *
     * 좌표는 소수점 3자리로 반올림 (~100m 정밀도)
     * - 100m 이내 위치에서의 요청은 같은 캐시 활용
     * - 검색 반경(1~5km)을 고려하면 충분한 정밀도
     *
     * @param request 검색 요청
     * @return 캐시 키
     */
    public static String generateSearchKey(ParkingSearchRequest request) {
        // 소수점 3자리 반올림 (~100m 정밀도)
        double roundedLat = Math.round(request.getLat() * 1000) / 1000.0;
        double roundedLng = Math.round(request.getLng() * 1000) / 1000.0;

        // 반경 반올림 (0.5km 단위)
        double roundedRadius = Math.round(request.getRadius() * 2) / 2.0;

        // 필터 해시
        String filterHash = generateFilterHash(request);

        return String.format("%.3f:%.3f:%.1f:%s:%d:%d",
                roundedLat,
                roundedLng,
                roundedRadius,
                filterHash,
                request.getPage(),
                request.getSize()
        );
    }

    /**
     * 필터 조건 해시 생성
     */
    private static String generateFilterHash(ParkingSearchRequest request) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(request.getAvailable())) {
            sb.append("A");
        }
        if (Boolean.TRUE.equals(request.getFree())) {
            sb.append("F");
        }
        return sb.length() > 0 ? sb.toString() : "N";
    }

    /**
     * 위도/경도를 Geohash로 인코딩
     * 간소화된 버전 - 실제 프로덕션에서는 Geohash 라이브러리 사용 권장
     */
    public static String encodeGeohash(double lat, double lng, int precision) {
        String base32 = "0123456789bcdefghjkmnpqrstuvwxyz";
        StringBuilder geohash = new StringBuilder();

        double minLat = -90.0, maxLat = 90.0;
        double minLng = -180.0, maxLng = 180.0;
        boolean isEven = true;
        int bit = 0;
        int ch = 0;

        while (geohash.length() < precision) {
            if (isEven) {
                double mid = (minLng + maxLng) / 2;
                if (lng >= mid) {
                    ch |= (1 << (4 - bit));
                    minLng = mid;
                } else {
                    maxLng = mid;
                }
            } else {
                double mid = (minLat + maxLat) / 2;
                if (lat >= mid) {
                    ch |= (1 << (4 - bit));
                    minLat = mid;
                } else {
                    maxLat = mid;
                }
            }

            isEven = !isEven;
            if (bit < 4) {
                bit++;
            } else {
                geohash.append(base32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }

        return geohash.toString();
    }

    /**
     * 주차장 상세 정보 캐시 키 생성
     */
    public static String generateDetailKey(Long parkingId) {
        return "detail:" + parkingId;
    }

    /**
     * 주차장 가용성 캐시 키 생성
     */
    public static String generateAvailabilityKey(Long parkingId) {
        return "avail:" + parkingId;
    }
}
