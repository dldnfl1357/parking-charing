package com.example.api.dto;

import com.example.common.domain.entity.Facility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ChargerResponse from(Facility facility) {
        return from(facility, null);
    }

    public static ChargerResponse from(Facility facility, Double distance) {
        ChargerResponseBuilder builder = ChargerResponse.builder()
                .id(facility.getId())
                .externalId(facility.getExternalId())
                .name(facility.getName())
                .address(facility.getAddress())
                .latitude(facility.getLocation().getLatitude())
                .longitude(facility.getLocation().getLongitude())
                .distance(distance != null ? Math.round(distance * 100.0) / 100.0 : null);

        // extraInfo 파싱
        if (facility.getExtraInfo() != null) {
            try {
                JsonNode node = objectMapper.readTree(facility.getExtraInfo());
                builder.chgerType(getTextValue(node, "chgerType"))
                        .chgerTypeDesc(getTextValue(node, "chgerTypeDesc"))
                        .stat(getTextValue(node, "stat"))
                        .statDesc(getTextValue(node, "statDesc"))
                        .output(getTextValue(node, "output"))
                        .method(getTextValue(node, "method"))
                        .useTime(getTextValue(node, "useTime"))
                        .busiNm(getTextValue(node, "busiNm"))
                        .busiCall(getTextValue(node, "busiCall"))
                        .parkingFree(getTextValue(node, "parkingFree"));
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        return builder.build();
    }

    private static String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}
