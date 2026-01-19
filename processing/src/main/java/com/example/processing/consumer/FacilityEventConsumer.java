package com.example.processing.consumer;

import com.example.common.domain.entity.Facility;
import com.example.common.event.FacilityEvent;
import com.example.processing.service.FacilityService;
import com.example.processing.service.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 시설 이벤트 소비자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacilityEventConsumer {

    private final FacilityService facilityService;
    private final IndexingService indexingService;

    @KafkaListener(topics = "${kafka.topic.facility}", groupId = "processing-group")
    public void consume(FacilityEvent event) {
        log.debug("Received facility event: {} - {}", event.getEventType(), event.getExternalId());

        try {
            switch (event.getEventType()) {
                case UPSERT -> {
                    Facility facility = facilityService.saveOrUpdate(event);
                    indexingService.index(facility);
                }
                case DELETE -> {
                    indexingService.delete(event.getExternalId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process facility event: {}", event.getExternalId(), e);
        }
    }
}
