package com.example.api.controller;

import com.example.api.dto.ParkingResponse;
import com.example.api.dto.ParkingSearchRequest;
import com.example.api.service.ParkingSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주차장 검색 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/parkings")
@RequiredArgsConstructor
public class ParkingSearchController {

    private final ParkingSearchService parkingSearchService;

    /**
     * 주차장 검색 (ElasticSearch)
     * GET /api/v1/parkings/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<ParkingResponse>> search(ParkingSearchRequest request) {
        List<ParkingResponse> result = parkingSearchService.search(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 주차장 검색 (MySQL) - 벤치마크용
     * GET /api/v1/parkings/search/mysql
     *
     * 사용 예시:
     * - 기본 검색: /search/mysql?lat=37.5&lng=127.0
     * - 반경 지정: /search/mysql?lat=37.5&lng=127.0&radius=3
     * - 가용만: /search/mysql?lat=37.5&lng=127.0&available=true
     * - 페이징: /search/mysql?lat=37.5&lng=127.0&page=0&size=10
     */
    @GetMapping("/search/mysql")
    public ResponseEntity<List<ParkingResponse>> searchByMysql(ParkingSearchRequest request) {
        List<ParkingResponse> result = parkingSearchService.searchByMysql(request);
        return ResponseEntity.ok(result);
    }
}
