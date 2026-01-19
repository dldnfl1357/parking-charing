package com.example.collection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 전기차 충전소 상태 API 응답 DTO
 * API: http://apis.data.go.kr/B552584/EvCharger/getChargerStatus
 *
 * 최근 N분 내 상태가 변경된 충전기만 반환
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChargingStatusResponse {

    private String resultCode;
    private String resultMsg;
    private int numOfRows;
    private int pageNo;
    private int totalCount;
    private Items items;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<StatusItem> item;

        public List<StatusItem> getItem() {
            return item != null ? item : Collections.emptyList();
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusItem {
        private String statId;      // 충전소 ID
        private String chgerId;     // 충전기 ID
        private String stat;        // 충전기 상태 (0~5)
        private String statUpdDt;   // 상태 갱신 일시 (yyyyMMddHHmmss)

        /**
         * 외부 ID 생성 (statId + chgerId)
         */
        public String getExternalId() {
            return statId + "_" + chgerId;
        }

        /**
         * 사용 가능 여부 (stat=2)
         */
        public boolean isAvailable() {
            return "2".equals(stat);
        }

        /**
         * 가용 수량 (사용 가능이면 1, 아니면 0)
         */
        public int getAvailableCount() {
            return isAvailable() ? 1 : 0;
        }
    }

    public List<StatusItem> getItemList() {
        return items != null ? items.getItem() : Collections.emptyList();
    }

    public boolean isSuccess() {
        return "00".equals(resultCode);
    }
}
