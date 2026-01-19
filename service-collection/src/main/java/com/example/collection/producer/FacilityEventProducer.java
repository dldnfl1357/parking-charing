package com.example.collection.producer;

import com.example.common.event.FacilityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 이벤트 발행자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacilityEventProducer {

    private final KafkaTemplate<String, FacilityEvent> kafkaTemplate;

    @Value("${kafka.topic.facility}")
    private String facilityTopic;

    /**
     * 시설 이벤트 발행
     */
    public void publish(FacilityEvent event) {
        String key = event.getExternalId();

        CompletableFuture<SendResult<String, FacilityEvent>> future =
                kafkaTemplate.send(facilityTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event for {}: {}", key, ex.getMessage());
            } else {
                log.debug("Sent event for {} to partition {} offset {}",
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
