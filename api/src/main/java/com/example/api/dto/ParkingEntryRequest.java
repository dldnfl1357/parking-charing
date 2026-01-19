package com.example.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 입차 요청 DTO
 */
@Getter
@NoArgsConstructor
public class ParkingEntryRequest {

    @NotNull(message = "시설 ID는 필수입니다")
    private Long facilityId;

    @NotBlank(message = "차량번호는 필수입니다")
    private String vehicleNumber;
}
