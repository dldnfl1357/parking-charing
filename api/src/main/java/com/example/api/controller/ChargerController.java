package com.example.api.controller;

import com.example.api.dto.ChargerResponse;
import com.example.api.dto.ChargerSearchRequest;
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
     *
     * 사용 예시:
     * - 기본 검색: /search?lat=37.5&lng=127.0
     * - 반경 지정: /search?lat=37.5&lng=127.0&radius=3
     * - 가용만: /search?lat=37.5&lng=127.0&available=true
     * - 주차무료: /search?lat=37.5&lng=127.0&parkingFree=true
     * - 충전기타입: /search?lat=37.5&lng=127.0&chgerType=07
     * - 페이징: /search?lat=37.5&lng=127.0&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<List<ChargerResponse>> search(ChargerSearchRequest request) {
        List<ChargerResponse> result = chargerService.search(request);
        return ResponseEntity.ok(result);
    }
}
