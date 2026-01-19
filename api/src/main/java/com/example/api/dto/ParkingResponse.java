package com.example.api.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 주차장 응답 DTO
 */
@Getter
@Builder
public class ParkingResponse {

    private Long id;
    private String externalId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private Double distance;  // km (검색 시)

    // 주차장 정보
    private int totalCount;       // 총 주차면
    private int availableCount;   // 가용 주차면

    // 요금 정보
    private String baseFee;       // 기본 요금 정보
    private String addFee;        // 추가 요금 정보
    private String dayMaxCrg;     // 1일 최대 요금

    // 운영 정보
    private String weekdayOperTime;  // 평일 운영 시간
    private String source;           // 데이터 출처 (TS: 한국교통안전공단)
}
