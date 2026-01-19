package com.example.collection.client;

import com.example.collection.dto.ParkingInfoResponse;
import com.example.collection.dto.ParkingOprResponse;
import com.example.collection.dto.ParkingRealtimeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 한국교통안전공단 주차장 API 클라이언트
 * API: http://apis.data.go.kr/B553881/Parking
 */
@Slf4j
@Component
public class TsParkingApiClient extends BaseApiClient {

    private static final String BASE_URL = "http://apis.data.go.kr/B553881/Parking";

    @Value("${collection.parking-api.api-key}")
    private String apiKey;

    public TsParkingApiClient(WebClient webClient, ObjectMapper objectMapper) {
        super(webClient, objectMapper);
    }

    /**
     * 주차장 시설정보 조회
     * API: /PrkSttusInfo
     */
    public ParkingInfoResponse fetchParkingInfo(int pageNo, int numOfRows) {
        log.info("Fetching parking info: pageNo={}, numOfRows={}", pageNo, numOfRows);

        String url = buildUrl(BASE_URL, "/PrkSttusInfo", apiKey, pageNo, numOfRows);
        ParkingInfoResponse response = fetchAndParse(url, ParkingInfoResponse.class);

        if (response != null && response.isSuccess()) {
            log.info("Fetched {} parking lots (total: {})",
                    response.getItemList().size(), response.getTotalCount());
        } else {
            log.warn("Parking Info API returned error: {}",
                    response != null ? response.getResultMsg() : "null response");
        }

        return response;
    }

    /**
     * 주차장 운영정보 조회
     * API: /PrkOprInfo
     */
    public ParkingOprResponse fetchParkingOpr(int pageNo, int numOfRows) {
        log.info("Fetching parking operation info: pageNo={}, numOfRows={}", pageNo, numOfRows);

        String url = buildUrl(BASE_URL, "/PrkOprInfo", apiKey, pageNo, numOfRows);
        ParkingOprResponse response = fetchAndParse(url, ParkingOprResponse.class);

        if (response != null && response.isSuccess()) {
            log.info("Fetched {} parking operation records (total: {})",
                    response.getItemList().size(), response.getTotalCount());
        } else {
            log.warn("Parking Operation API returned error: {}",
                    response != null ? response.getResultMsg() : "null response");
        }

        return response;
    }

    /**
     * 주차장 실시간정보 조회
     * API: /PrkRealtimeInfo
     */
    public ParkingRealtimeResponse fetchParkingRealtime(int pageNo, int numOfRows) {
        log.info("Fetching parking realtime info: pageNo={}, numOfRows={}", pageNo, numOfRows);

        String url = buildUrl(BASE_URL, "/PrkRealtimeInfo", apiKey, pageNo, numOfRows);
        ParkingRealtimeResponse response = fetchAndParse(url, ParkingRealtimeResponse.class);

        if (response != null && response.isSuccess()) {
            log.info("Fetched {} parking realtime records (total: {})",
                    response.getItemList().size(), response.getTotalCount());
        } else {
            log.warn("Parking Realtime API returned error: {}",
                    response != null ? response.getResultMsg() : "null response");
        }

        return response;
    }
}
