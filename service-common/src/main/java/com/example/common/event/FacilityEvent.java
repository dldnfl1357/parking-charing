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
     *
     * 공공 API는 updatedAt을 제공하지 않으므로 해시 기반 변경 감지 사용:
     * - 시설정보/운영정보: 필드 해시 비교 → 변경 시 이벤트 발행
     * - 실시간정보: 값 직접 비교 → 변경 시 이벤트 발행
     */
    public enum EventType {
        FACILITY_CREATED,     // 신규 시설 등록
        FACILITY_UPDATED,     // 시설정보 변경 (이름, 주소, 좌표, 총주차면)
        OPERATION_UPDATED,    // 운영정보 변경 (요금, 운영시간)
        AVAILABILITY_CHANGED, // 가용성 변경 (availableCount)
        FACILITY_DELETED,     // 시설 삭제
        UPSERT,               // 전체 정보 생성/수정 (하위 호환)
        STATUS_UPDATE         // 상태만 업데이트 (하위 호환)
    }

    /**
     * 상태 업데이트 이벤트 생성 (부분 업데이트용)
     */
    public static FacilityEvent statusUpdate(String externalId, int availableCount, LocalDateTime collectedAt) {
        return FacilityEvent.builder()
                .eventType(EventType.STATUS_UPDATE)
                .externalId(externalId)
                .availableCount(availableCount)
                .collectedAt(collectedAt)
                .build();
    }

    /**
     * 가용성 변경 이벤트 생성 (실시간 정보용)
     */
    public static FacilityEvent availabilityChanged(String externalId, int availableCount, LocalDateTime collectedAt) {
        return FacilityEvent.builder()
                .eventType(EventType.AVAILABILITY_CHANGED)
                .externalId(externalId)
                .availableCount(availableCount)
                .collectedAt(collectedAt)
                .build();
    }

    /**
     * 운영정보 변경 이벤트 생성
     */
    public static FacilityEvent operationUpdated(String externalId, String extraInfo, LocalDateTime collectedAt) {
        return FacilityEvent.builder()
                .eventType(EventType.OPERATION_UPDATED)
                .externalId(externalId)
                .extraInfo(extraInfo)
                .collectedAt(collectedAt)
                .build();
    }
}
