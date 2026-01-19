package com.example.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 주차장 검색 요청 DTO
 */
@Getter
@Setter
public class ParkingSearchRequest {

    private Double lat;           // 위도
    private Double lng;           // 경도
    private Double radius = 5.0;  // 반경 (km), 기본 5km

    private Boolean available;    // 주차 가능 여부만
    private Boolean free;         // 무료 주차장만

    private int page = 0;
    private int size = 20;
}
