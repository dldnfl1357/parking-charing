package com.example.collection.scheduler;

import com.example.collection.client.ChargingApiClient;
import com.example.collection.client.TsParkingApiClient;
import com.example.collection.dto.ChargingApiResponse;
import com.example.collection.dto.ChargingStatusResponse;
import com.example.collection.dto.ParkingInfoResponse;
import com.example.collection.dto.ParkingOprResponse;
import com.example.collection.dto.ParkingRealtimeResponse;
import com.example.collection.producer.FacilityEventProducer;
import com.example.collection.service.ChangeDetectionService;
import com.example.collection.translator.FacilityTranslator;
import com.example.common.event.FacilityEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 데이터 수집 스케줄러
 *
 * 수집 전략:
 * 1. 메타 정보: 1일 1회 새벽 - 위치, 이름, 타입 등 거의 안 바뀌는 정보
 * 2. 상태 정보: 5분마다 - 변경된 상태만 조회하여 부분 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionScheduler {

    private final ChargingApiClient chargingApiClient;
    private final TsParkingApiClient tsParkingApiClient;
    private final FacilityTranslator facilityTranslator;
    private final FacilityEventProducer facilityEventProducer;
    private final ChangeDetectionService changeDetectionService;
    private final ObjectMapper objectMapper;

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 100;
    private static final int STATUS_PERIOD_MINUTES = 5;

    /**
     * 충전소 메타 정보 수집 (1일 1회, 새벽 3시 30분)
     */
    @Scheduled(cron = "0 30 3 * * *")
    public void collectChargingInfo() {
        log.info("Starting charging info collection (daily full sync)");

        int pageNo = 1;
        int totalCollected = 0;

        try {
            while (pageNo <= MAX_PAGES) {
                ChargingApiResponse response = chargingApiClient.fetchChargingData(pageNo, PAGE_SIZE);

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

            log.info("Completed charging info collection: {} records", totalCollected);
        } catch (Exception e) {
            log.error("Error during charging info collection", e);
        }
    }

    /**
     * 충전소 상태 업데이트 (5분마다)
     */
    @Scheduled(fixedRate = 300000, initialDelay = 300000)
    public void collectChargingStatus() {
        log.info("Starting charging status collection (delta update, period={}min)", STATUS_PERIOD_MINUTES);

        int pageNo = 1;
        int totalCollected = 0;

        try {
            while (true) {
                ChargingStatusResponse response = chargingApiClient.fetchChargingStatus(
                        STATUS_PERIOD_MINUTES, pageNo, PAGE_SIZE);

                if (response == null || response.getItemList().isEmpty()) {
                    break;
                }

                LocalDateTime now = LocalDateTime.now();
                List<FacilityEvent> events = response.getItemList().stream()
                        .map(item -> FacilityEvent.statusUpdate(
                                item.getExternalId(),
                                item.getAvailableCount(),
                                now))
                        .toList();

                events.forEach(facilityEventProducer::publish);
                totalCollected += events.size();

                log.debug("Page {}: {} status updates", pageNo, events.size());

                if (response.getItemList().size() < PAGE_SIZE ||
                    pageNo * PAGE_SIZE >= response.getTotalCount()) {
                    break;
                }

                pageNo++;
            }

            log.info("Completed charging status collection: {} updates", totalCollected);
        } catch (Exception e) {
            log.error("Error during charging status collection", e);
        }
    }

    /**
     * 주차장 메타 정보 수집 (1일 1회, 새벽 4시) - 한국교통안전공단
     *
     * 변경 감지 전략:
     * - 시설정보: 이름, 주소, 좌표, 총주차면 해시 비교
     * - 운영정보: 요금, 운영시간 해시 비교
     * - 변경된 항목만 이벤트 발행
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void collectParkingInfoTS() {
        log.info("Starting TS parking info collection (daily with change detection)");

        try {
            // 1단계: 시설정보 수집
            Map<String, ParkingInfoResponse.ParkingInfoItem> infoMap = collectAllParkingInfo();
            log.info("Collected {} parking info records", infoMap.size());

            // 2단계: 운영정보 수집
            Map<String, ParkingOprResponse.ParkingOprItem> oprMap = collectAllParkingOpr();
            log.info("Collected {} parking operation records", oprMap.size());

            // 3단계: 변경 감지 후 이벤트 발행
            int facilityCreated = 0;
            int facilityUpdated = 0;
            int operationUpdated = 0;
            int unchanged = 0;

            LocalDateTime now = LocalDateTime.now();

            for (Map.Entry<String, ParkingInfoResponse.ParkingInfoItem> entry : infoMap.entrySet()) {
                String externalId = "TS_" + entry.getKey();
                ParkingInfoResponse.ParkingInfoItem infoItem = entry.getValue();
                ParkingOprResponse.ParkingOprItem oprItem = oprMap.get(entry.getKey());

                if (!infoItem.isValid()) {
                    continue;
                }

                // 시설정보 변경 감지
                boolean isFacilityChanged = changeDetectionService.isFacilityInfoChanged(
                        externalId,
                        infoItem.getName(),
                        infoItem.getAddress(),
                        infoItem.getLatitude(),
                        infoItem.getLongitude(),
                        infoItem.getTotalCount()
                );

                // 운영정보 변경 감지
                String operationHash = buildOperationHashInput(oprItem);
                boolean isOperationChanged = changeDetectionService.isOperationInfoChanged(
                        externalId, operationHash);

                if (isFacilityChanged && !changeDetectionService.facilityExists(externalId)) {
                    // 신규 시설 - 전체 정보 이벤트 발행
                    FacilityEvent event = facilityTranslator.translateParkingTS(infoItem, oprItem);
                    if (event != null) {
                        facilityEventProducer.publish(event);
                        facilityCreated++;
                    }
                } else {
                    // 기존 시설
                    if (isFacilityChanged) {
                        // 시설정보만 변경 이벤트 발행
                        FacilityEvent event = facilityTranslator.translateParkingTS(infoItem, oprItem);
                        if (event != null) {
                            facilityEventProducer.publish(event);
                            facilityUpdated++;
                        }
                    } else if (isOperationChanged && oprItem != null) {
                        // 운영정보만 변경 이벤트 발행
                        String extraInfo = buildOperationExtraInfo(oprItem);
                        FacilityEvent event = FacilityEvent.operationUpdated(externalId, extraInfo, now);
                        facilityEventProducer.publish(event);
                        operationUpdated++;
                    } else {
                        unchanged++;
                    }
                }
            }

            log.info("Completed TS parking info collection: created={}, facilityUpdated={}, " +
                            "operationUpdated={}, unchanged={}",
                    facilityCreated, facilityUpdated, operationUpdated, unchanged);
        } catch (Exception e) {
            log.error("Error during TS parking info collection", e);
        }
    }

    private String buildOperationHashInput(ParkingOprResponse.ParkingOprItem oprItem) {
        if (oprItem == null) {
            return "";
        }
        return oprItem.getBaseFeeInfo() + "|" + oprItem.getAddFeeInfo() + "|" +
                oprItem.getWeekdayOperTime() + "|" +
                (oprItem.getDayMaxCrg() != null ? oprItem.getDayMaxCrg() : "");
    }

    private String buildOperationExtraInfo(ParkingOprResponse.ParkingOprItem oprItem) {
        try {
            Map<String, Object> extraInfo = new HashMap<>();
            extraInfo.put("baseFee", oprItem.getBaseFeeInfo());
            extraInfo.put("addFee", oprItem.getAddFeeInfo());
            extraInfo.put("weekdayOperTime", oprItem.getWeekdayOperTime());
            if (oprItem.getDayMaxCrg() != null) {
                extraInfo.put("dayMaxCrg", oprItem.getDayMaxCrg());
            }
            return objectMapper.writeValueAsString(extraInfo);
        } catch (JsonProcessingException e) {
            log.warn("Failed to build operation extraInfo", e);
            return "{}";
        }
    }

    /**
     * 주차장 실시간 정보 수집 (5분마다) - 한국교통안전공단
     *
     * 변경 감지 전략:
     * - availableCount 값 직접 비교
     * - 변경된 주차장만 이벤트 발행
     */
    @Scheduled(fixedRate = 300000, initialDelay = 300000)
    public void collectParkingRealtimeTS() {
        log.info("Starting TS parking realtime collection (with change detection)");

        int pageNo = 1;
        int changedCount = 0;
        int unchangedCount = 0;

        try {
            while (pageNo <= MAX_PAGES) {
                ParkingRealtimeResponse response = tsParkingApiClient.fetchParkingRealtime(pageNo, PAGE_SIZE);

                if (response == null || response.getItemList().isEmpty()) {
                    break;
                }

                LocalDateTime now = LocalDateTime.now();

                for (ParkingRealtimeResponse.ParkingRealtimeItem item : response.getItemList()) {
                    String externalId = "TS_" + item.getExternalId();
                    int availableCount = item.getAvailableCount();

                    // 변경 감지 - 값 직접 비교
                    if (changeDetectionService.isAvailabilityChanged(externalId, availableCount)) {
                        FacilityEvent event = FacilityEvent.availabilityChanged(
                                externalId, availableCount, now);
                        facilityEventProducer.publish(event);
                        changedCount++;
                    } else {
                        unchangedCount++;
                    }
                }

                log.debug("Page {}: processed {} items", pageNo, response.getItemList().size());

                if (response.getItemList().size() < PAGE_SIZE ||
                    pageNo * PAGE_SIZE >= response.getTotalCount()) {
                    break;
                }

                pageNo++;
            }

            log.info("Completed TS parking realtime collection: changed={}, unchanged={}",
                    changedCount, unchangedCount);
        } catch (Exception e) {
            log.error("Error during TS parking realtime collection", e);
        }
    }

    private Map<String, ParkingInfoResponse.ParkingInfoItem> collectAllParkingInfo() {
        Map<String, ParkingInfoResponse.ParkingInfoItem> result = new HashMap<>();
        int pageNo = 1;

        while (pageNo <= MAX_PAGES) {
            ParkingInfoResponse response = tsParkingApiClient.fetchParkingInfo(pageNo, PAGE_SIZE);

            if (response == null || response.getItemList().isEmpty()) {
                break;
            }

            for (ParkingInfoResponse.ParkingInfoItem item : response.getItemList()) {
                if (item.getExternalId() != null) {
                    result.put(item.getExternalId(), item);
                }
            }

            if (response.getItemList().size() < PAGE_SIZE ||
                pageNo * PAGE_SIZE >= response.getTotalCount()) {
                break;
            }

            pageNo++;
        }

        return result;
    }

    private Map<String, ParkingOprResponse.ParkingOprItem> collectAllParkingOpr() {
        Map<String, ParkingOprResponse.ParkingOprItem> result = new HashMap<>();
        int pageNo = 1;

        while (pageNo <= MAX_PAGES) {
            ParkingOprResponse response = tsParkingApiClient.fetchParkingOpr(pageNo, PAGE_SIZE);

            if (response == null || response.getItemList().isEmpty()) {
                break;
            }

            for (ParkingOprResponse.ParkingOprItem item : response.getItemList()) {
                if (item.getExternalId() != null) {
                    result.put(item.getExternalId(), item);
                }
            }

            if (response.getItemList().size() < PAGE_SIZE ||
                pageNo * PAGE_SIZE >= response.getTotalCount()) {
                break;
            }

            pageNo++;
        }

        return result;
    }
}
