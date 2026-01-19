package com.example.collection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공공 API 응답 DTO
 */
@Getter
@NoArgsConstructor
public class PublicApiResponse<T> {

    @JsonProperty("currentCount")
    private int currentCount;

    @JsonProperty("data")
    private List<T> data;

    @JsonProperty("matchCount")
    private int matchCount;

    @JsonProperty("page")
    private int page;

    @JsonProperty("perPage")
    private int perPage;

    @JsonProperty("totalCount")
    private int totalCount;

    @Getter
    @NoArgsConstructor
    public static class ParkingData {
        @JsonProperty("주차장코드")
        private String parkingCode;

        @JsonProperty("주차장명")
        private String parkingName;

        @JsonProperty("주소")
        private String address;

        @JsonProperty("위도")
        private Double latitude;

        @JsonProperty("경도")
        private Double longitude;

        @JsonProperty("주차구획수")
        private Integer totalSlots;

        @JsonProperty("현재주차차량수")
        private Integer currentVehicles;

        @JsonProperty("기본주차시간")
        private Integer baseMinutes;

        @JsonProperty("기본요금")
        private Integer baseFee;

        @JsonProperty("추가단위시간")
        private Integer unitMinutes;

        @JsonProperty("추가단위요금")
        private Integer unitFee;

        @JsonProperty("일최대요금")
        private Integer dailyMaxFee;
    }

    @Getter
    @NoArgsConstructor
    public static class ChargingData {
        @JsonProperty("충전소코드")
        private String chargingCode;

        @JsonProperty("충전소명")
        private String chargingName;

        @JsonProperty("주소")
        private String address;

        @JsonProperty("위도")
        private Double latitude;

        @JsonProperty("경도")
        private Double longitude;

        @JsonProperty("충전기수")
        private Integer totalChargers;

        @JsonProperty("사용가능충전기수")
        private Integer availableChargers;

        @JsonProperty("충전기타입")
        private String chargerType;

        @JsonProperty("충전용량")
        private String chargingCapacity;
    }
}
