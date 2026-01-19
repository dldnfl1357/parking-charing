package com.example.api.controller;

import com.example.api.dto.ChargerResponse;
import com.example.api.dto.ChargerSearchRequest;
import com.example.api.dto.ChargerStatsResponse;
import com.example.api.service.ChargerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 충전소 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/chargers")
@RequiredArgsConstructor
public class ChargerController {

    private final ChargerService chargerService;

    /**
     * 충전소 검색
     * GET /api/v1/chargers/search?lat=37.5&lng=127.0&radius=5&chgerType=01&available=true&parkingFree=true&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<List<ChargerResponse>> search(ChargerSearchRequest request) {
        List<ChargerResponse> result = chargerService.search(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 충전소 상세 조회
     * GET /api/v1/chargers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChargerResponse> findById(@PathVariable Long id) {
        ChargerResponse result = chargerService.findById(id);
        return ResponseEntity.ok(result);
    }

    /**
     * 내 주변 충전소 (거리순)
     * GET /api/v1/chargers/nearby?lat=37.5&lng=127.0&radiusKm=3&limit=10
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<ChargerResponse>> findNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        List<ChargerResponse> result = chargerService.findNearby(lat, lng, radiusKm, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * 사용 가능한 충전소
     * GET /api/v1/chargers/available?lat=37.5&lng=127.0&radiusKm=5&limit=20
     */
    @GetMapping("/available")
    public ResponseEntity<List<ChargerResponse>> findAvailable(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "20") int limit) {
        List<ChargerResponse> result = chargerService.findAvailable(lat, lng, radiusKm, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * 충전소 통계
     * GET /api/v1/chargers/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ChargerStatsResponse> getStats() {
        ChargerStatsResponse result = chargerService.getStats();
        return ResponseEntity.ok(result);
    }
}
