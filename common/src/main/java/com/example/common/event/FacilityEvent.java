package com.example.common.event;

import com.example.common.domain.FacilityType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    private String externalId;
    private FacilityType type;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int totalCount;
    private int availableCount;
    private String extraInfo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime collectedAt;

    /**
     * 이벤트 유형
     */
    public enum EventType {
        UPSERT,
        DELETE
    }
}
