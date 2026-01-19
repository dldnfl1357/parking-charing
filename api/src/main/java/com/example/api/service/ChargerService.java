package com.example.api.service;

import com.example.api.document.ChargerDocument;
import com.example.api.dto.ChargerResponse;
import com.example.api.dto.ChargerSearchRequest;
import com.example.api.repository.ChargerSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 충전소 서비스 (ElasticSearch 기반)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargerService {

    private final ChargerSearchRepository chargerSearchRepository;

    /**
     * 위치 기반 충전소 검색 (ES)
     */
    public List<ChargerResponse> search(ChargerSearchRequest request) {
        SearchHits<ChargerDocument> hits = chargerSearchRepository.search(
                request.getLat(),
                request.getLng(),
                request.getRadius(),
                request.getChgerType(),
                request.getAvailable(),
                request.getParkingFree(),
                request.getPage(),
                request.getSize()
        );

        return hits.getSearchHits().stream()
                .map(this::toResponse)
                .toList();
    }

    private ChargerResponse toResponse(SearchHit<ChargerDocument> hit) {
        ChargerDocument doc = hit.getContent();

        // 거리 정보 추출 (geo_distance 정렬 시)
        Double distance = null;
        List<Object> sortValues = hit.getSortValues();
        if (!sortValues.isEmpty() && sortValues.get(0) instanceof Number) {
            distance = ((Number) sortValues.get(0)).doubleValue() / 1000.0; // m → km
        }

        return ChargerResponse.builder()
                .id(doc.getId() != null ? Long.parseLong(doc.getId()) : null)
                .externalId(doc.getExternalId())
                .name(doc.getName())
                .address(doc.getAddress())
                .latitude(doc.getLocation() != null ? doc.getLocation().getLat() : 0)
                .longitude(doc.getLocation() != null ? doc.getLocation().getLon() : 0)
                .distance(distance != null ? Math.round(distance * 100.0) / 100.0 : null)
                .build();
    }
}
