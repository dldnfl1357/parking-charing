package com.example.common.event;

import com.example.common.domain.FacilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka로 발행되는 시설 이벤트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private EventType eventType = EventType.UPSERT;

    private FacilityType facilityType;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private FacilityPayload payload;

    /**
     * 이벤트 유형
     */
    public enum EventType {
        UPSERT,
        DELETE
    }

    /**
     * 시설 정보 페이로드
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacilityPayload {
        private String externalId;
        private String name;
        private String address;
        private double latitude;
        private double longitude;
        private int totalCount;
        private int availableCount;
        private Map<String, Object> extraInfo;
    }
}
