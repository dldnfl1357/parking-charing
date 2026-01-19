package com.example.common.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가용성 값 객체
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Availability {

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "available_count", nullable = false)
    private int availableCount;

    public Availability(int totalCount, int availableCount) {
        validate(totalCount, availableCount);
        this.totalCount = totalCount;
        this.availableCount = availableCount;
    }

    private void validate(int totalCount, int availableCount) {
        if (totalCount < 0) {
            throw new IllegalArgumentException("Total count cannot be negative");
        }
        if (availableCount < 0) {
            throw new IllegalArgumentException("Available count cannot be negative");
        }
        if (availableCount > totalCount) {
            throw new IllegalArgumentException("Available count cannot exceed total count");
        }
    }

    /**
     * 점유율 (0.0 ~ 1.0)
     */
    public double getOccupancyRate() {
        if (totalCount == 0) return 0.0;
        return (double) (totalCount - availableCount) / totalCount;
    }

    /**
     * 점유율 백분율 (0 ~ 100)
     */
    public int getOccupancyPercentage() {
        return (int) Math.round(getOccupancyRate() * 100);
    }

    /**
     * 만차 여부
     */
    public boolean isFull() {
        return totalCount > 0 && availableCount == 0;
    }

    /**
     * 혼잡도 레벨
     */
    public CongestionLevel getCongestionLevel() {
        if (isFull()) return CongestionLevel.FULL;
        if (getOccupancyRate() >= 0.8) return CongestionLevel.CROWDED;
        if (getOccupancyRate() >= 0.5) return CongestionLevel.MODERATE;
        return CongestionLevel.EMPTY;
    }

    /**
     * 혼잡도 레벨 Enum
     */
    public enum CongestionLevel {
        EMPTY("여유", "#4CAF50"),
        MODERATE("보통", "#FFC107"),
        CROWDED("혼잡", "#FF9800"),
        FULL("만차", "#F44336");

        private final String label;
        private final String color;

        CongestionLevel(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }
    }

    @Override
    public String toString() {
        return String.format("Availability(%d/%d, %s)",
                availableCount, totalCount, getCongestionLevel().getLabel());
    }
}
