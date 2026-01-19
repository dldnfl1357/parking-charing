package com.example.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주차 이벤트 DTO (Kafka)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingEvent {

    private Long sessionId;
    private Long facilityId;
    private String vehicleNumber;
    private EventType eventType;
    private BigDecimal feeAmount;
    private String paymentMethod;
    private String paymentStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime entryTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime exitTime;

    public enum EventType {
        ENTRY,      // 입차
        EXIT,       // 출차
        PAYMENT     // 결제
    }

    // === 팩토리 메서드 ===

    public static ParkingEvent entry(Long sessionId, Long facilityId, String vehicleNumber, LocalDateTime entryTime) {
        return ParkingEvent.builder()
                .sessionId(sessionId)
                .facilityId(facilityId)
                .vehicleNumber(vehicleNumber)
                .eventType(EventType.ENTRY)
                .entryTime(entryTime)
                .eventTime(LocalDateTime.now())
                .build();
    }

    public static ParkingEvent exit(Long sessionId, Long facilityId, String vehicleNumber,
                                    LocalDateTime entryTime, LocalDateTime exitTime, BigDecimal feeAmount) {
        return ParkingEvent.builder()
                .sessionId(sessionId)
                .facilityId(facilityId)
                .vehicleNumber(vehicleNumber)
                .eventType(EventType.EXIT)
                .entryTime(entryTime)
                .exitTime(exitTime)
                .feeAmount(feeAmount)
                .eventTime(LocalDateTime.now())
                .build();
    }

    public static ParkingEvent payment(Long sessionId, Long facilityId, BigDecimal amount,
                                       String paymentMethod, String paymentStatus) {
        return ParkingEvent.builder()
                .sessionId(sessionId)
                .facilityId(facilityId)
                .eventType(EventType.PAYMENT)
                .feeAmount(amount)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .eventTime(LocalDateTime.now())
                .build();
    }
}
