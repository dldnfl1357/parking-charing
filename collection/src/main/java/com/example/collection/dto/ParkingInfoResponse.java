package com.example.collection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 주차장 시설정보 API 응답 DTO
 * API: http://apis.data.go.kr/B553881/Parking/PrkSttusInfo
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParkingInfoResponse {

    private String resultCode;
    private String resultMsg;
    private int numOfRows;
    private int pageNo;
    private int totalCount;

    @JsonProperty("PrkSttusInfo")
    private List<ParkingInfoItem> prkSttusInfo;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParkingInfoItem {
        @JsonProperty("prk_center_id")
        private String prkCenterId;         // 주차장 관리 ID

        @JsonProperty("prk_plce_nm")
        private String prkPlceNm;           // 주차장명

        @JsonProperty("prk_plce_adres")
        private String prkPlceAdres;        // 주차장 도로명 주소

        @JsonProperty("prk_plce_entrc_la")
        private String prkPlceEntrcLa;      // 위도

        @JsonProperty("prk_plce_entrc_lo")
        private String prkPlceEntrcLo;      // 경도

        @JsonProperty("prk_cmprt_co")
        private Integer prkCmprtCo;         // 총 주차 구획 수

        public String getExternalId() {
            return prkCenterId;
        }

        public String getName() {
            return prkPlceNm;
        }

        public String getAddress() {
            return prkPlceAdres;
        }

        public double getLatitude() {
            try {
                return prkPlceEntrcLa != null ? Double.parseDouble(prkPlceEntrcLa) : 0.0;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        public double getLongitude() {
            try {
                return prkPlceEntrcLo != null ? Double.parseDouble(prkPlceEntrcLo) : 0.0;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        public int getTotalCount() {
            return prkCmprtCo != null ? prkCmprtCo : 0;
        }

        public boolean isValid() {
            return prkCenterId != null &&
                   getLatitude() != 0.0 && getLongitude() != 0.0;
        }
    }

    public List<ParkingInfoItem> getItemList() {
        return prkSttusInfo != null ? prkSttusInfo : Collections.emptyList();
    }

    public boolean isSuccess() {
        return "00".equals(resultCode);
    }
}
