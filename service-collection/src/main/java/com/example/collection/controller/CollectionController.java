package com.example.collection.controller;

import com.example.collection.scheduler.CollectionScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 수집 테스트용 컨트롤러
 * 운영 환경에서는 비활성화 권장
 */
@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionScheduler collectionScheduler;

    /**
     * 충전소 메타 정보 수동 수집
     */
    @PostMapping("/charging/info")
    public ResponseEntity<String> triggerChargingInfo() {
        collectionScheduler.collectChargingInfo();
        return ResponseEntity.ok("Charging info collection triggered");
    }

    /**
     * 충전소 상태 수동 수집
     */
    @PostMapping("/charging/status")
    public ResponseEntity<String> triggerChargingStatus() {
        collectionScheduler.collectChargingStatus();
        return ResponseEntity.ok("Charging status collection triggered");
    }

    /**
     * 주차장 메타 정보 수동 수집
     */
    @PostMapping("/parking/info")
    public ResponseEntity<String> triggerParkingInfo() {
        collectionScheduler.collectParkingInfoTS();
        return ResponseEntity.ok("Parking info collection triggered");
    }

    /**
     * 주차장 실시간 정보 수동 수집
     */
    @PostMapping("/parking/realtime")
    public ResponseEntity<String> triggerParkingRealtime() {
        collectionScheduler.collectParkingRealtimeTS();
        return ResponseEntity.ok("Parking realtime collection triggered");
    }
}
