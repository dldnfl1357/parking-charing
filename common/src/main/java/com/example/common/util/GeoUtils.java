package com.example.common.util;

/**
 * 지리 관련 유틸리티
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
        // Utility class
    }

    /**
     * 두 좌표 간의 거리를 계산 (Haversine 공식)
     *
     * @param lat1 첫 번째 위도
     * @param lon1 첫 번째 경도
     * @param lat2 두 번째 위도
     * @param lon2 두 번째 경도
     * @return 거리 (km)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 좌표가 유효한 범위인지 확인
     */
    public static boolean isValidCoordinate(double latitude, double longitude) {
        return latitude >= -90.0 && latitude <= 90.0 &&
                longitude >= -180.0 && longitude <= 180.0;
    }
}
