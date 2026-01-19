package com.example.collection.translator;

import com.example.collection.dto.ChargingApiResponse;
import com.example.collection.dto.PublicApiResponse;
import com.example.common.domain.FacilityType;
import com.example.common.event.FacilityEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ACL - 공공 API 데이터를 도메인 이벤트로 변환
 */
@Component
@RequiredArgsConstructor
public class FacilityTranslator {

    private final ObjectMapper objectMapper;

    /**
     * 주차장 데이터 → FacilityEvent
     */
    public FacilityEvent translateParking(PublicApiResponse.ParkingData data) {
        if (data.getParkingCode() == null || data.getLatitude() == null || data.getLongitude() == null) {
            return null;
        }

        int totalCount = data.getTotalSlots() != null ? data.getTotalSlots() : 0;
        int currentVehicles = data.getCurrentVehicles() != null ? data.getCurrentVehicles() : 0;
        int availableCount = Math.max(0, totalCount - currentVehicles);

        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("baseFee", data.getBaseFee());
        extraInfo.put("baseMinutes", data.getBaseMinutes());
        extraInfo.put("unitFee", data.getUnitFee());
        extraInfo.put("unitMinutes", data.getUnitMinutes());
        extraInfo.put("dailyMaxFee", data.getDailyMaxFee());

        return FacilityEvent.builder()
                .externalId(data.getParkingCode())
                .type(FacilityType.PARKING)
                .name(data.getParkingName())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .address(data.getAddress())
                .totalCount(totalCount)
                .availableCount(availableCount)
                .extraInfo(toJson(extraInfo))
                .collectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 충전소 데이터 → FacilityEvent (한국환경공단 API)
     */
    public FacilityEvent translateCharging(ChargingApiResponse.ChargerItem item) {
        if (!item.isValid()) {
            return null;
        }

        double lat, lng;
        try {
            lat = Double.parseDouble(item.getLat());
            lng = Double.parseDouble(item.getLng());
        } catch (NumberFormatException e) {
            return null;
        }

        // 충전소ID + 충전기ID로 유니크 키 생성
        String externalId = item.getStatId() + "-" + item.getChgerId();

        // 사용 가능하면 1, 아니면 0
        int availableCount = item.isAvailable() ? 1 : 0;

        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("chgerType", item.getChgerType());
        extraInfo.put("chgerTypeDesc", item.getChargerTypeDescription());
        extraInfo.put("stat", item.getStat());
        extraInfo.put("statDesc", item.getStatusDescription());
        extraInfo.put("output", item.getOutput());
        extraInfo.put("method", item.getMethod());
        extraInfo.put("useTime", item.getUseTime());
        extraInfo.put("busiNm", item.getBusiNm());
        extraInfo.put("busiCall", item.getBusiCall());
        extraInfo.put("parkingFree", item.getParkingFree());

        String address = item.getAddr();
        if (item.getAddrDetail() != null && !item.getAddrDetail().isBlank()) {
            address += " " + item.getAddrDetail();
        }

        return FacilityEvent.builder()
                .externalId(externalId)
                .type(FacilityType.CHARGING)
                .name(item.getStatNm())
                .latitude(lat)
                .longitude(lng)
                .address(address)
                .totalCount(1)  // 충전기 1대
                .availableCount(availableCount)
                .extraInfo(toJson(extraInfo))
                .collectedAt(LocalDateTime.now())
                .build();
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
