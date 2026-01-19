package com.example.collection.client;

import com.example.collection.dto.ChargingApiResponse;
import com.example.collection.dto.ChargingStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 한국환경공단 전기차 충전소 API 클라이언트
 * API: http://apis.data.go.kr/B552584/EvCharger
 */
@Slf4j
@Component
public class ChargingApiClient extends BaseApiClient {

    private static final String BASE_URL = "http://apis.data.go.kr/B552584/EvCharger";

    @Value("${collection.charging-api.api-key}")
    private String apiKey;

    public ChargingApiClient(WebClient webClient, ObjectMapper objectMapper) {
        super(webClient, objectMapper);
    }

    /**
     * 충전소 정보 조회 - 메타 정보 전체
     * API: /getChargerInfo
     */
    public ChargingApiResponse fetchChargingData(int pageNo, int numOfRows) {
        log.info("Fetching charging data: pageNo={}, numOfRows={}", pageNo, numOfRows);

        try {
            String url = String.format("%s/getChargerInfo?serviceKey=%s&pageNo=%d&numOfRows=%d&dataType=JSON",
                    BASE_URL, apiKey, pageNo, numOfRows);

            ChargingApiResponse response = webClient.get()
                    .uri(URI.create(url))
                    .accept(MediaType.ALL)
                    .retrieve()
                    .bodyToMono(ChargingApiResponse.class)
                    .onErrorResume(e -> {
                        log.error("Failed to fetch charging data: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("Fetched {} chargers (total: {})",
                        response.getItemList().size(), response.getTotalCount());
                return response;
            } else {
                log.warn("Charging API returned error: {}",
                        response != null ? response.getResultMsg() : "null response");
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching charging data", e);
            return null;
        }
    }

    /**
     * 충전소 상태 조회 - 변경된 상태만
     * API: /getChargerStatus
     *
     * @param period 상태갱신 조회 범위 (분, 기본값 5, 최대 10)
     */
    public ChargingStatusResponse fetchChargingStatus(int period, int pageNo, int numOfRows) {
        log.info("Fetching charging status: period={}min, pageNo={}, numOfRows={}", period, pageNo, numOfRows);

        try {
            String url = String.format("%s/getChargerStatus?serviceKey=%s&period=%d&pageNo=%d&numOfRows=%d&dataType=JSON",
                    BASE_URL, apiKey, period, pageNo, numOfRows);

            ChargingStatusResponse response = webClient.get()
                    .uri(URI.create(url))
                    .accept(MediaType.ALL)
                    .retrieve()
                    .bodyToMono(ChargingStatusResponse.class)
                    .onErrorResume(e -> {
                        log.error("Failed to fetch charging status: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("Fetched {} status updates (total: {})",
                        response.getItemList().size(), response.getTotalCount());
                return response;
            } else {
                log.warn("Charging Status API returned error: {}",
                        response != null ? response.getResultMsg() : "null response");
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching charging status", e);
            return null;
        }
    }
}
