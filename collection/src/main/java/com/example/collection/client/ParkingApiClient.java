package com.example.collection.client;

import com.example.collection.dto.ChargingApiResponse;
import com.example.collection.dto.PublicApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * 공공 API 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingApiClient {

    private final WebClient webClient;

    @Value("${collection.parking-api.base-url}")
    private String parkingApiBaseUrl;

    @Value("${collection.parking-api.api-key}")
    private String parkingApiKey;

    @Value("${collection.charging-api.base-url}")
    private String chargingApiBaseUrl;

    @Value("${collection.charging-api.api-key}")
    private String chargingApiKey;

    /**
     * 주차장 정보 조회
     */
    public List<PublicApiResponse.ParkingData> fetchParkingData(int page, int perPage) {
        try {
            PublicApiResponse<PublicApiResponse.ParkingData> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.odcloud.kr")
                            .path("/api/15050093/v1/uddi:29c13dd8-be1f-4f34-b638-c8b7333f22f6")
                            .queryParam("page", page)
                            .queryParam("perPage", perPage)
                            .queryParam("serviceKey", parkingApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<PublicApiResponse<PublicApiResponse.ParkingData>>() {})
                    .onErrorResume(e -> {
                        log.error("Failed to fetch parking data: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching parking data", e);
            return Collections.emptyList();
        }
    }

    /**
     * 충전소 정보 조회 (한국환경공단 API)
     * API: http://apis.data.go.kr/B552584/EvCharger/getChargerInfo
     */
    public ChargingApiResponse fetchChargingData(int pageNo, int numOfRows) {
        try {
            log.info("Fetching charging data: pageNo={}, numOfRows={}", pageNo, numOfRows);

            ChargingApiResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("http")
                            .host("apis.data.go.kr")
                            .path("/B552584/EvCharger/getChargerInfo")
                            .queryParam("serviceKey", chargingApiKey)
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("dataType", "JSON")
                            .build())
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
}
