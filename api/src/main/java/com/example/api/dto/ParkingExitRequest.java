package com.example.api.dto;

import com.example.common.domain.entity.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출차 요청 DTO
 */
@Getter
@NoArgsConstructor
public class ParkingExitRequest {

    @NotNull(message = "세션 ID는 필수입니다")
    private Long sessionId;

    @NotNull(message = "결제 방법은 필수입니다")
    private Payment.PaymentMethod paymentMethod;
}
