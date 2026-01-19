package com.example.api.dto;

import com.example.common.domain.entity.ParkingSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 입차 응답 DTO
 */
@Getter
@Builder
public class ParkingEntryResponse {

    private Long sessionId;
    private Long facilityId;
    private String vehicleNumber;
    private LocalDateTime entryTime;
    private String status;

    public static ParkingEntryResponse from(ParkingSession session) {
        return ParkingEntryResponse.builder()
                .sessionId(session.getId())
                .facilityId(session.getFacilityId())
                .vehicleNumber(session.getVehicleNumber().getValue())
                .entryTime(session.getEntryTime())
                .status(session.getStatus().name())
                .build();
    }
}
