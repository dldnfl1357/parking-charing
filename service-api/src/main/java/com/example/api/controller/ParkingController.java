package com.example.api.controller;

import com.example.api.dto.*;
import com.example.api.service.ParkingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주차 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingSessionService parkingSessionService;

    /**
     * 입차 API
     * POST /api/v1/parking/entry
     */
    @PostMapping("/entry")
    public ResponseEntity<ParkingEntryResponse> entry(@Valid @RequestBody ParkingEntryRequest request) {
        ParkingEntryResponse response = parkingSessionService.entry(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 출차 + 결제 API
     * POST /api/v1/parking/exit
     */
    @PostMapping("/exit")
    public ResponseEntity<ParkingExitResponse> exit(@Valid @RequestBody ParkingExitRequest request) {
        ParkingExitResponse response = parkingSessionService.exit(request);
        return ResponseEntity.ok(response);
    }
}
