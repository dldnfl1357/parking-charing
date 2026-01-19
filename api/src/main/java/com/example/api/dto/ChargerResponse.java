package com.example.api.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 충전소 응답 DTO
 */
@Getter
@Builder
public class ChargerResponse {

    private Long id;
    private String externalId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private Double distance;  // km (검색 시)

    // 충전기 정보
    private String chgerType;
    private String chgerTypeDesc;
    private String stat;
    private String statDesc;
    private String output;        // 충전용량 kW
    private String method;        // 충전방식

    // 운영 정보
    private String useTime;
    private String busiNm;
    private String busiCall;
    private String parkingFree;
}
