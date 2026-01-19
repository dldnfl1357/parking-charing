package com.example.api.service;

import com.example.common.domain.entity.Payment;
import com.example.common.domain.vo.Money;
import com.example.api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 서비스 (Mock PG)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 결제 처리
     */
    @Transactional
    public Payment processPayment(Long parkingSessionId, Money amount, Payment.PaymentMethod paymentMethod) {
        log.info("Processing payment for session {}: {} via {}",
                parkingSessionId, amount, paymentMethod);

        Payment payment = Payment.create(parkingSessionId, amount, paymentMethod);
        payment = paymentRepository.save(payment);

        // Mock PG 처리
        try {
            String transactionId = mockPgProcess(amount, paymentMethod);
            payment.complete(transactionId);
            log.info("Payment completed: transactionId={}", transactionId);
        } catch (Exception e) {
            payment.fail(e.getMessage());
            log.error("Payment failed: {}", e.getMessage());
        }

        return paymentRepository.save(payment);
    }

    /**
     * Mock PG 처리
     */
    private String mockPgProcess(Money amount, Payment.PaymentMethod paymentMethod) {
        // 실제로는 PG사 API 연동
        // 여기서는 항상 성공하는 Mock
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 결제 환불
     */
    @Transactional
    public Payment refund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다"));

        payment.refund();
        return paymentRepository.save(payment);
    }
}
