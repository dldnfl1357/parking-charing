package com.example.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 충전소 검색 요청 DTO
 */
@Getter
@Setter
public class ChargerSearchRequest {

    private Double lat;           // 위도
    private Double lng;           // 경도
    private Double radius = 5.0;  // 반경 (km), 기본 5km

    private String chgerType;     // 충전기 타입 (01~11)
    private Boolean available;    // 사용 가능 여부만
    private Boolean parkingFree;  // 주차료 무료만

    private int page = 0;
    private int size = 20;
}
