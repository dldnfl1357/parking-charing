package com.example.collection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 전기차 충전소 API 응답 DTO
 * API: http://apis.data.go.kr/B552584/EvCharger/getChargerInfo
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChargingApiResponse {

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
        private List<ChargerItem> item;

        public List<ChargerItem> getItem() {
            return item != null ? item : Collections.emptyList();
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChargerItem {
        private String statNm;      // 충전소명
        private String statId;      // 충전소ID
        private String chgerId;     // 충전기ID
        private String chgerType;   // 충전기타입 (01~11)
        private String addr;        // 주소
        private String addrDetail;  // 주소상세
        private String lat;         // 위도
        private String lng;         // 경도
        private String useTime;     // 이용가능시간
        private String busiId;      // 기관 아이디
        private String busiNm;      // 운영기관명
        private String busiCall;    // 운영기관 연락처
        private String stat;        // 충전기상태 (0~5)
        private String statUpdDt;   // 상태갱신일시
        private String output;      // 충전용량 kW
        private String method;      // 충전방식 (단독/동시)
        private String zcode;       // 지역코드
        private String parkingFree; // 주차료무료 (Y/N)
        private String limitYn;     // 이용자 제한 (Y/N)
        private String delYn;       // 삭제 여부 (Y/N)

        /**
         * 사용 가능 여부 (stat=2)
         */
        public boolean isAvailable() {
            return "2".equals(stat);
        }

        /**
         * 삭제되지 않은 유효한 데이터인지
         */
        public boolean isValid() {
            return !"Y".equals(delYn) && lat != null && lng != null;
        }

        /**
         * 충전기 타입 설명
         */
        public String getChargerTypeDescription() {
            return switch (chgerType) {
                case "01" -> "DC차데모";
                case "02" -> "AC완속";
                case "03" -> "DC차데모+AC3상";
                case "04" -> "DC콤보";
                case "05" -> "DC차데모+DC콤보";
                case "06" -> "DC차데모+AC3상+DC콤보";
                case "07" -> "AC3상";
                case "08" -> "DC콤보(완속)";
                case "09" -> "NACS";
                case "10" -> "DC콤보+NACS";
                case "11" -> "DC콤보2(버스전용)";
                default -> "기타";
            };
        }

        /**
         * 충전기 상태 설명
         */
        public String getStatusDescription() {
            return switch (stat) {
                case "1" -> "통신이상";
                case "2" -> "사용가능";
                case "3" -> "충전중";
                case "4" -> "운영중지";
                case "5" -> "점검중";
                default -> "알수없음";
            };
        }
    }

    public List<ChargerItem> getItemList() {
        return items != null ? items.getItem() : Collections.emptyList();
    }

    public boolean isSuccess() {
        return "00".equals(resultCode);
    }
}
