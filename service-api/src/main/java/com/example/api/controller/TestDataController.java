package com.example.api.controller;

import com.example.api.document.ParkingDocument;
import com.example.common.domain.FacilityType;
import com.example.common.domain.entity.Facility;
import com.example.api.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 테스트 데이터 생성 컨트롤러 (개발용)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestDataController {

    private final FacilityRepository facilityRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final double MIN_LAT = 37.25;
    private static final double MAX_LAT = 37.70;
    private static final double MIN_LNG = 126.65;
    private static final double MAX_LNG = 127.20;

    /**
     * 대용량 테스트 데이터 생성
     * POST /api/v1/test/generate?count=1000000&batchSize=5000
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTestData(
            @RequestParam(defaultValue = "1000000") int count,
            @RequestParam(defaultValue = "5000") int batchSize,
            @RequestParam(defaultValue = "true") boolean mysql,
            @RequestParam(defaultValue = "true") boolean elasticsearch) {

        log.info("Generating {} test records (batch={})", count, batchSize);
        long startTime = System.currentTimeMillis();

        Random random = new Random();
        AtomicInteger mysqlCount = new AtomicInteger(0);
        AtomicInteger esCount = new AtomicInteger(0);

        int batches = (count + batchSize - 1) / batchSize;

        for (int batch = 0; batch < batches; batch++) {
            int start = batch * batchSize;
            int end = Math.min(start + batchSize, count);
            int currentBatchSize = end - start;

            List<Facility> facilities = new ArrayList<>(currentBatchSize);
            List<ParkingDocument> documents = new ArrayList<>(currentBatchSize);

            for (int i = start; i < end; i++) {
                double lat = MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble();
                double lng = MIN_LNG + (MAX_LNG - MIN_LNG) * random.nextDouble();
                int totalCount = 50 + random.nextInt(450);
                int availableCount = random.nextInt(totalCount + 1);

                String externalId = "TEST_" + System.currentTimeMillis() + "_" + i;
                String name = "테스트주차장_" + i;
                String address = "서울시 테스트구 테스트동 " + (i % 1000);

                String extraInfo = String.format(
                        "{\"baseFee\":%d,\"baseMinutes\":30,\"unitFee\":%d,\"unitMinutes\":10,\"source\":\"TEST\"}",
                        1000 + random.nextInt(2000),
                        300 + random.nextInt(500)
                );

                if (mysql) {
                    Facility facility = Facility.createParking(
                            externalId, name, lat, lng, address,
                            totalCount, availableCount, extraInfo
                    );
                    facilities.add(facility);
                }

                if (elasticsearch) {
                    ParkingDocument doc = ParkingDocument.builder()
                            .id(externalId)
                            .externalId(externalId)
                            .type(FacilityType.PARKING.name())
                            .name(name)
                            .address(address)
                            .location(new GeoPoint(lat, lng))
                            .totalCount(totalCount)
                            .availableCount(availableCount)
                            .extraInfo(extraInfo)
                            .collectedAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    documents.add(doc);
                }
            }

            // MySQL 배치 저장
            if (mysql && !facilities.isEmpty()) {
                facilityRepository.saveAll(facilities);
                mysqlCount.addAndGet(facilities.size());
            }

            // ES 배치 저장
            if (elasticsearch && !documents.isEmpty()) {
                elasticsearchOperations.save(documents);
                esCount.addAndGet(documents.size());
            }

            if ((batch + 1) % 10 == 0 || batch == batches - 1) {
                log.info("Progress: {}/{} batches completed ({} records)",
                        batch + 1, batches, (batch + 1) * batchSize);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> result = Map.of(
                "mysqlInserted", mysqlCount.get(),
                "esInserted", esCount.get(),
                "elapsedMs", elapsed,
                "recordsPerSecond", count * 1000L / Math.max(elapsed, 1)
        );

        log.info("Test data generation completed: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * 테스트 데이터 삭제
     * DELETE /api/v1/test/cleanup
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupTestData() {
        log.info("Cleaning up test data...");
        long startTime = System.currentTimeMillis();

        // TEST_ 로 시작하는 데이터 삭제 (MySQL)
        // 주의: 실제 프로덕션에서는 사용하지 않음
        long deleted = facilityRepository.deleteByExternalIdStartingWith("TEST_");

        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> result = Map.of(
                "deleted", deleted,
                "elapsedMs", elapsed
        );

        log.info("Cleanup completed: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * 현재 데이터 수 확인
     * GET /api/v1/test/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCount() {
        long mysqlCount = facilityRepository.count();

        return ResponseEntity.ok(Map.of(
                "mysql", mysqlCount
        ));
    }
}
