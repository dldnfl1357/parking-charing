package com.example.processing.controller;

import com.example.processing.service.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 인덱싱 관리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/indexing")
@RequiredArgsConstructor
public class IndexingController {

    private final IndexingService indexingService;

    /**
     * 전체 재인덱싱
     * POST /api/v1/indexing/reindex
     */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindexAll() {
        log.info("Reindex request received");

        int count = indexingService.reindexAll();

        return ResponseEntity.ok(Map.of(
                "status", "completed",
                "indexedCount", count
        ));
    }
}
