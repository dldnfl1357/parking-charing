package com.example.api.service;

import com.example.api.document.ChargerDocument;
import com.example.api.dto.ChargerResponse;
import com.example.api.dto.ChargerSearchRequest;
import com.example.api.dto.ChargerStatsResponse;
import com.example.api.repository.ChargerSearchRepository;
import com.example.api.repository.FacilityRepository;
import com.example.common.domain.FacilityType;
import com.example.common.domain.entity.Facility;
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
    private final FacilityRepository facilityRepository;  // ID 조회용

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

    /**
     * 내 주변 충전소 (거리순, ES)
     */
    public List<ChargerResponse> findNearby(double lat, double lng, double radiusKm, int limit) {
        SearchHits<ChargerDocument> hits = chargerSearchRepository.findNearby(lat, lng, radiusKm, limit);

        return hits.getSearchHits().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 사용 가능한 충전소만 조회 (ES)
     */
    public List<ChargerResponse> findAvailable(Double lat, Double lng, double radiusKm, int limit) {
        SearchHits<ChargerDocument> hits = chargerSearchRepository.findAvailable(lat, lng, radiusKm, limit);

        return hits.getSearchHits().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 충전소 상세 조회 (DB - 정확한 데이터)
     */
    public ChargerResponse findById(Long id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("충전소를 찾을 수 없습니다: " + id));

        if (facility.getType() != FacilityType.CHARGING) {
            throw new IllegalArgumentException("해당 시설은 충전소가 아닙니다: " + id);
        }

        return ChargerResponse.from(facility);
    }

    /**
     * 충전소 통계 (ES Aggregation)
     */
    public ChargerStatsResponse getStats() {
        ChargerSearchRepository.ChargerStats stats = chargerSearchRepository.getStats();

        return ChargerStatsResponse.builder()
                .totalCount(stats.totalCount)
                .availableCount(stats.availableCount)
                .chargingCount(0)  // ES에서 extraInfo 파싱이 어려워 별도 처리 필요
                .unavailableCount(stats.unavailableCount)
                .byChargerType(null)  // 추후 구현
                .byRegion(stats.byRegion)
                .build();
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
