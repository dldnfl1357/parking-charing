package com.example.collection.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

/**
 * 공공 API 클라이언트 베이스 클래스
 * - text/html Content-Type 처리
 * - 공통 요청/응답 처리
 */
@Slf4j
public abstract class BaseApiClient {

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    protected BaseApiClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * GET 요청 후 JSON 파싱
     * 공공 API가 text/html로 응답하는 경우 처리
     */
    protected <T> T fetchAndParse(String url, Class<T> responseType) {
        try {
            String jsonStr = webClient.get()
                    .uri(URI.create(url))
                    .accept(MediaType.ALL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (jsonStr == null || jsonStr.isBlank()) {
                log.warn("Empty response from: {}", url);
                return null;
            }

            return objectMapper.readValue(jsonStr, responseType);
        } catch (Exception e) {
            log.error("Failed to fetch and parse from {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * API 키를 포함한 URL 생성
     */
    protected String buildUrl(String baseUrl, String path, String apiKey, int pageNo, int numOfRows) {
        return String.format("%s%s?serviceKey=%s&pageNo=%d&numOfRows=%d&format=2",
                baseUrl, path, apiKey, pageNo, numOfRows);
    }
}
