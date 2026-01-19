package com.example.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 충전소 통계 응답 DTO
 */
@Getter
@Builder
public class ChargerStatsResponse {

    private long totalCount;           // 전체 충전기 수
    private long availableCount;       // 사용 가능
    private long chargingCount;        // 충전 중
    private long unavailableCount;     // 사용 불가

    private Map<String, Long> byChargerType;  // 타입별 통계
    private Map<String, Long> byRegion;       // 지역별 통계
}
