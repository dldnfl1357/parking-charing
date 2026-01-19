package com.example.api.dto;

import com.example.common.domain.entity.Payment;
import com.example.common.domain.entity.ParkingSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 출차 응답 DTO
 */
@Getter
@Builder
public class ParkingExitResponse {

    private Long sessionId;
    private Long facilityId;
    private String vehicleNumber;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String parkingDuration;
    private long feeAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;

    public static ParkingExitResponse from(ParkingSession session, Payment payment) {
        return ParkingExitResponse.builder()
                .sessionId(session.getId())
                .facilityId(session.getFacilityId())
                .vehicleNumber(session.getVehicleNumber().getValue())
                .entryTime(session.getEntryTime())
                .exitTime(session.getExitTime())
                .parkingDuration(session.getParkingDurationString())
                .feeAmount(session.getFee().toLong())
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentStatus(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .build();
    }
}
