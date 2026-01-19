package com.example.common.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * 차량번호 값 객체
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class VehicleNumber {

    private static final Pattern KOREAN_PLATE_PATTERN = Pattern.compile("^\\d{2,3}[가-힣]\\d{4}$");

    @Column(name = "vehicle_number", length = 20)
    private String value;

    private VehicleNumber(String value) {
        String normalized = normalize(value);
        if (!isValid(normalized)) {
            throw new IllegalArgumentException("유효하지 않은 차량번호입니다: " + value);
        }
        this.value = normalized;
    }

    public static VehicleNumber of(String value) {
        return new VehicleNumber(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("차량번호는 필수입니다");
        }
        return value.replaceAll("\\s+", "").replaceAll("-", "");
    }

    private static boolean isValid(String value) {
        return KOREAN_PLATE_PATTERN.matcher(value).matches();
    }

    public String getMasked() {
        if (value.length() < 4) return value;
        return value.substring(0, value.length() - 4) + "****";
    }

    @Override
    public String toString() {
        return value;
    }
}
