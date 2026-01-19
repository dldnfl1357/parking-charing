package com.example.common.domain.entity;

import com.example.common.domain.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 엔티티
 */
@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_session", columnList = "parking_session_id"),
                @Index(name = "idx_payment_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parking_session_id", nullable = false)
    private Long parkingSessionId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount"))
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum PaymentMethod {
        CREDIT_CARD,
        KAKAO_PAY,
        NAVER_PAY,
        TOSS_PAY
    }

    public enum PaymentStatus {
        PENDING,    // 결제 대기
        COMPLETED,  // 결제 완료
        FAILED,     // 결제 실패
        REFUNDED    // 환불
    }

    // === 팩토리 메서드 ===

    public static Payment create(Long parkingSessionId, Money amount, PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.parkingSessionId = parkingSessionId;
        payment.amount = amount;
        payment.paymentMethod = paymentMethod;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = LocalDateTime.now();
        payment.updatedAt = LocalDateTime.now();
        return payment;
    }

    // === 도메인 로직 ===

    /**
     * 결제 완료 처리
     */
    public void complete(String transactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 결제만 완료 처리할 수 있습니다");
        }
        this.transactionId = transactionId;
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리
     */
    public void fail(String reason) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 결제만 실패 처리할 수 있습니다");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 환불 처리
     */
    public void refund() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 결제만 환불할 수 있습니다");
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 완료 여부
     */
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }
}
