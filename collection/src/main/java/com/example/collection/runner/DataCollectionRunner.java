package com.example.collection.runner;

import com.example.collection.scheduler.CollectionScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 데이터 수집 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCollectionRunner implements ApplicationRunner {

    private final CollectionScheduler collectionScheduler;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Starting initial data collection ===");

        // 주차장 메타 정보 수집
        try {
            log.info("Collecting parking info...");
            collectionScheduler.collectParkingInfoTS();
        } catch (Exception e) {
            log.error("Failed to collect parking info on startup", e);
        }
    }
}
