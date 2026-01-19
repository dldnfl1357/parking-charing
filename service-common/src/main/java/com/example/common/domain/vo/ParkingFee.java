package com.example.common.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주차 요금 값 객체
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ParkingFee {

    @Column(name = "base_fee")
    private int baseFee;

    @Column(name = "base_minutes")
    private int baseMinutes = 30;

    @Column(name = "unit_fee")
    private int unitFee;

    @Column(name = "unit_minutes")
    private int unitMinutes = 10;

    @Column(name = "daily_max_fee")
    private Integer dailyMaxFee;

    public ParkingFee(int baseFee, int baseMinutes, int unitFee, int unitMinutes, Integer dailyMaxFee) {
        this.baseFee = baseFee;
        this.baseMinutes = baseMinutes;
        this.unitFee = unitFee;
        this.unitMinutes = unitMinutes;
        this.dailyMaxFee = dailyMaxFee;
    }

    /**
     * 주차 시간에 따른 예상 요금 계산
     *
     * @param minutes 주차 시간 (분)
     * @return 예상 요금 (원)
     */
    public int calculate(int minutes) {
        if (minutes <= 0) return 0;
        if (minutes <= baseMinutes) return baseFee;

        int additionalMinutes = minutes - baseMinutes;
        int additionalUnits = (additionalMinutes + unitMinutes - 1) / unitMinutes;
        int calculatedFee = baseFee + (additionalUnits * unitFee);

        if (dailyMaxFee != null) {
            return Math.min(calculatedFee, dailyMaxFee);
        }
        return calculatedFee;
    }

    /**
     * 시간당 요금
     */
    public int getHourlyRate() {
        return calculate(60);
    }

    /**
     * 무료 여부
     */
    public boolean isFree() {
        return baseFee == 0 && unitFee == 0;
    }

    @Override
    public String toString() {
        if (isFree()) return "무료";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("최초 %d분 %d원", baseMinutes, baseFee));
        if (unitFee > 0) {
            sb.append(String.format(", 이후 %d분당 %d원", unitMinutes, unitFee));
        }
        if (dailyMaxFee != null) {
            sb.append(String.format(" (일 최대 %d원)", dailyMaxFee));
        }
        return sb.toString();
    }
}
