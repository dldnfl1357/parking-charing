package com.example.common.domain.vo;

import com.example.common.util.GeoUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 위치 좌표 값 객체
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Location {

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    public Location(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude != 0.0 || longitude != 0.0) {
            if (latitude < -90.0 || latitude > 90.0) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90");
            }
            if (longitude < -180.0 || longitude > 180.0) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180");
            }
        }
    }

    /**
     * 다른 위치와의 거리 계산 (km)
     */
    public double distanceTo(Location other) {
        return GeoUtils.calculateDistance(
                this.latitude, this.longitude,
                other.latitude, other.longitude
        );
    }

    public boolean isValid() {
        return latitude != 0.0 && longitude != 0.0;
    }

    @Override
    public String toString() {
        return String.format("Location(lat=%.6f, lng=%.6f)", latitude, longitude);
    }
}
