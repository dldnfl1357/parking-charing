package com.example.collection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 주차장 실시간정보 API 응답 DTO
 * API: http://apis.data.go.kr/B553881/Parking/PrkRealtimeInfo
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParkingRealtimeResponse {

    private String resultCode;
    private String resultMsg;
    private int numOfRows;
    private int pageNo;
    private int totalCount;

    @JsonProperty("PrkRealtimeInfo")
    private List<ParkingRealtimeItem> prkRealtimeInfo;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParkingRealtimeItem {
        @JsonProperty("prk_center_id")
        private String prkCenterId;         // 주차장 관리 ID

        @JsonProperty("prk_now_cnt")
        private Integer prkNowCnt;          // 현재 주차 대수

        @JsonProperty("prk_cmprt_co")
        private Integer prkCmprtCo;         // 총 주차 구획 수

        @JsonProperty("updt_dt")
        private String updtDt;              // 갱신 일시

        public String getExternalId() {
            return prkCenterId;
        }

        public int getTotalCount() {
            return prkCmprtCo != null ? prkCmprtCo : 0;
        }

        public int getAvailableCount() {
            int total = getTotalCount();
            int current = prkNowCnt != null ? prkNowCnt : 0;
            return Math.max(0, total - current);
        }
    }

    public List<ParkingRealtimeItem> getItemList() {
        return prkRealtimeInfo != null ? prkRealtimeInfo : Collections.emptyList();
    }

    public boolean isSuccess() {
        return "00".equals(resultCode);
    }
}
