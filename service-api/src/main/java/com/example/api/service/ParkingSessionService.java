package com.example.api.service;

import com.example.common.domain.entity.Facility;
import com.example.common.domain.entity.Payment;
import com.example.common.domain.entity.ParkingSession;
import com.example.common.domain.vo.ParkingFee;
import com.example.common.domain.vo.VehicleNumber;
import com.example.common.event.ParkingEvent;
import com.example.api.dto.*;
import com.example.api.repository.FacilityRepository;
import com.example.api.repository.ParkingSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주차 세션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSessionService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final FacilityRepository facilityRepository;
    private final PaymentService paymentService;
    private final KafkaTemplate<String, ParkingEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 입차 처리
     */
    @Transactional
    public ParkingEntryResponse entry(ParkingEntryRequest request) {
        // 시설 확인
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new IllegalArgumentException("시설을 찾을 수 없습니다: " + request.getFacilityId()));

        // 만차 확인
        if (facility.isFull()) {
            throw new IllegalStateException("주차장이 만차입니다");
        }

        // 중복 입차 확인
        VehicleNumber vehicleNumber = VehicleNumber.of(request.getVehicleNumber());
        parkingSessionRepository.findByVehicleNumber_ValueAndStatus(
                vehicleNumber.getValue(), ParkingSession.ParkingStatus.ACTIVE)
                .ifPresent(s -> {
                    throw new IllegalStateException("이미 주차 중인 차량입니다: " + vehicleNumber.getValue());
                });

        // 요금 정책 파싱
        ParkingFee feePolicy = parseFeePolicy(facility.getExtraInfo());

        // 주차 세션 생성
        ParkingSession session = ParkingSession.createEntry(
                facility.getId(),
                vehicleNumber,
                feePolicy
        );
        session = parkingSessionRepository.save(session);

        // Kafka 이벤트 발행
        publishEntryEvent(session);

        log.info("Entry recorded: facilityId={}, vehicleNumber={}, sessionId={}",
                facility.getId(), vehicleNumber.getMasked(), session.getId());

        return ParkingEntryResponse.from(session);
    }

    /**
     * 출차 + 결제 처리
     */
    @Transactional
    public ParkingExitResponse exit(ParkingExitRequest request) {
        // 세션 확인
        ParkingSession session = parkingSessionRepository.findByIdAndStatus(
                        request.getSessionId(), ParkingSession.ParkingStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("활성 주차 세션을 찾을 수 없습니다: " + request.getSessionId()));

        // 출차 처리
        session.exit();
        session = parkingSessionRepository.save(session);

        // 결제 처리
        Payment payment = paymentService.processPayment(
                session.getId(),
                session.getFee(),
                request.getPaymentMethod()
        );

        // Kafka 이벤트 발행
        publishExitEvent(session, payment);

        log.info("Exit completed: sessionId={}, fee={}, paymentStatus={}",
                session.getId(), session.getFee(), payment.getStatus());

        return ParkingExitResponse.from(session, payment);
    }

    private ParkingFee parseFeePolicy(String extraInfo) {
        if (extraInfo == null || extraInfo.isBlank()) {
            return new ParkingFee(0, 30, 0, 10, null);
        }

        try {
            JsonNode node = objectMapper.readTree(extraInfo);
            int baseFee = node.has("baseFee") && !node.get("baseFee").isNull() ? node.get("baseFee").asInt() : 0;
            int baseMinutes = node.has("baseMinutes") && !node.get("baseMinutes").isNull() ? node.get("baseMinutes").asInt() : 30;
            int unitFee = node.has("unitFee") && !node.get("unitFee").isNull() ? node.get("unitFee").asInt() : 0;
            int unitMinutes = node.has("unitMinutes") && !node.get("unitMinutes").isNull() ? node.get("unitMinutes").asInt() : 10;
            Integer dailyMaxFee = node.has("dailyMaxFee") && !node.get("dailyMaxFee").isNull() ? node.get("dailyMaxFee").asInt() : null;

            return new ParkingFee(baseFee, baseMinutes, unitFee, unitMinutes, dailyMaxFee);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse fee policy: {}", extraInfo);
            return new ParkingFee(0, 30, 0, 10, null);
        }
    }

    private void publishEntryEvent(ParkingSession session) {
        ParkingEvent event = ParkingEvent.entry(
                session.getId(),
                session.getFacilityId(),
                session.getVehicleNumber().getMasked(),
                session.getEntryTime()
        );
        kafkaTemplate.send("parking-events", session.getId().toString(), event);
    }

    private void publishExitEvent(ParkingSession session, Payment payment) {
        ParkingEvent event = ParkingEvent.exit(
                session.getId(),
                session.getFacilityId(),
                session.getVehicleNumber().getMasked(),
                session.getEntryTime(),
                session.getExitTime(),
                session.getFee().getAmount()
        );
        kafkaTemplate.send("parking-events", session.getId().toString(), event);
    }
}
