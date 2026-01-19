package com.example.collection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 주차장 운영정보 API 응답 DTO
 * API: http://apis.data.go.kr/B553881/Parking/PrkOprInfo
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParkingOprResponse {

    private String resultCode;
    private String resultMsg;
    private int numOfRows;
    private int pageNo;
    private int totalCount;

    @JsonProperty("PrkOprInfo")
    private List<ParkingOprItem> prkOprInfo;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParkingOprItem {
        @JsonProperty("prk_center_id")
        private String prkCenterId;             // 주차장 관리 ID

        @JsonProperty("weekday_oper_bgng_tm")
        private String weekdayOperBgngTm;       // 평일 운영 시작 시간

        @JsonProperty("weekday_oper_end_tm")
        private String weekdayOperEndTm;        // 평일 운영 종료 시간

        @JsonProperty("sat_oper_bgng_tm")
        private String satOperBgngTm;           // 토요일 운영 시작 시간

        @JsonProperty("sat_oper_end_tm")
        private String satOperEndTm;            // 토요일 운영 종료 시간

        @JsonProperty("hldy_oper_bgng_tm")
        private String hldyOperBgngTm;          // 공휴일 운영 시작 시간

        @JsonProperty("hldy_oper_end_tm")
        private String hldyOperEndTm;           // 공휴일 운영 종료 시간

        @JsonProperty("bsc_prk_crg")
        private Integer bscPrkCrg;              // 기본 주차 요금

        @JsonProperty("bsc_prk_hr")
        private Integer bscPrkHr;               // 기본 주차 시간 (분)

        @JsonProperty("add_prk_crg")
        private Integer addPrkCrg;              // 추가 주차 요금

        @JsonProperty("add_prk_hr")
        private Integer addPrkHr;               // 추가 주차 단위 시간 (분)

        @JsonProperty("day_max_crg")
        private Integer dayMaxCrg;              // 1일 최대 요금

        public String getExternalId() {
            return prkCenterId;
        }

        public String getBaseFeeInfo() {
            if (bscPrkCrg == null || bscPrkHr == null) {
                return "정보없음";
            }
            if (bscPrkCrg == 0) {
                return "무료";
            }
            return String.format("%d분 %d원", bscPrkHr, bscPrkCrg);
        }

        public String getAddFeeInfo() {
            if (addPrkCrg == null || addPrkHr == null) {
                return "정보없음";
            }
            return String.format("%d분당 %d원", addPrkHr, addPrkCrg);
        }

        public String getWeekdayOperTime() {
            if (weekdayOperBgngTm == null || weekdayOperEndTm == null) {
                return "정보없음";
            }
            return weekdayOperBgngTm + " ~ " + weekdayOperEndTm;
        }
    }

    public List<ParkingOprItem> getItemList() {
        return prkOprInfo != null ? prkOprInfo : Collections.emptyList();
    }

    public boolean isSuccess() {
        return "00".equals(resultCode);
    }
}
