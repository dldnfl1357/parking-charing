package com.example.collection.scheduler;

import com.example.collection.client.ParkingApiClient;
import com.example.collection.dto.ChargingApiResponse;
import com.example.collection.dto.PublicApiResponse;
import com.example.collection.producer.FacilityEventProducer;
import com.example.collection.translator.FacilityTranslator;
import com.example.common.event.FacilityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 데이터 수집 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionScheduler {

    private final ParkingApiClient parkingApiClient;
    private final FacilityTranslator facilityTranslator;
    private final FacilityEventProducer facilityEventProducer;

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 10;  // 최대 1000개 (테스트용)

    /**
     * 주차장 데이터 수집 (5분마다)
     */
    @Scheduled(cron = "${collection.schedule.cron}")
    public void collectParkingData() {
        log.info("Starting parking data collection");

        int page = 1;
        int totalCollected = 0;

        try {
            while (page <= MAX_PAGES) {
                List<PublicApiResponse.ParkingData> dataList = parkingApiClient.fetchParkingData(page, PAGE_SIZE);

                if (dataList.isEmpty()) {
                    break;
                }

                List<FacilityEvent> events = dataList.stream()
                        .map(facilityTranslator::translateParking)
                        .filter(Objects::nonNull)
                        .toList();

                events.forEach(facilityEventProducer::publish);
                totalCollected += events.size();

                if (dataList.size() < PAGE_SIZE) {
                    break;
                }

                page++;
            }

            log.info("Completed parking data collection: {} records", totalCollected);
        } catch (Exception e) {
            log.error("Error during parking data collection", e);
        }
    }

    /**
     * 충전소 데이터 수집 (5분마다, 주차장 수집 후 30초 뒤)
     * 한국환경공단 전기차 충전소 API 사용
     */
    @Scheduled(cron = "30 */5 * * * *")
    public void collectChargingData() {
        log.info("Starting charging data collection");

        int pageNo = 1;
        int totalCollected = 0;

        try {
            while (pageNo <= MAX_PAGES) {
                ChargingApiResponse response = parkingApiClient.fetchChargingData(pageNo, PAGE_SIZE);

                if (response == null || response.getItemList().isEmpty()) {
                    break;
                }

                List<FacilityEvent> events = response.getItemList().stream()
                        .map(facilityTranslator::translateCharging)
                        .filter(Objects::nonNull)
                        .toList();

                events.forEach(facilityEventProducer::publish);
                totalCollected += events.size();

                log.info("Page {}: collected {} chargers", pageNo, events.size());

                if (response.getItemList().size() < PAGE_SIZE) {
                    break;
                }

                pageNo++;
            }

            log.info("Completed charging data collection: {} records", totalCollected);
        } catch (Exception e) {
            log.error("Error during charging data collection", e);
        }
    }
}
