package com.example.api.service;

import com.example.api.document.ParkingDocument;
import com.example.api.dto.ParkingResponse;
import com.example.api.dto.ParkingSearchRequest;
import com.example.api.repository.FacilityRepository;
import com.example.api.repository.ParkingSearchRepository;
import com.example.common.domain.FacilityType;
import com.example.common.domain.entity.Facility;
import com.example.common.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 주차장 검색 서비스 (MySQL / ElasticSearch)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSearchService {

    private final ParkingSearchRepository parkingSearchRepository;
    private final FacilityRepository facilityRepository;

    /**
     * 위치 기반 주차장 검색 (ES)
     */
    public List<ParkingResponse> search(ParkingSearchRequest request) {
        SearchHits<ParkingDocument> hits = parkingSearchRepository.search(
                request.getLat(),
                request.getLng(),
                request.getRadius(),
                request.getAvailable(),
                request.getFree(),
                request.getPage(),
                request.getSize()
        );

        return hits.getSearchHits().stream()
                .map(this::toResponse)
                .toList();
    }

    private ParkingResponse toResponse(SearchHit<ParkingDocument> hit) {
        ParkingDocument doc = hit.getContent();

        // 거리 정보 추출 (geo_distance 정렬 시)
        Double distance = null;
        List<Object> sortValues = hit.getSortValues();
        if (!sortValues.isEmpty() && sortValues.get(0) instanceof Number) {
            distance = ((Number) sortValues.get(0)).doubleValue();
        }

        return ParkingResponse.builder()
                .id(doc.getId() != null ? Long.parseLong(doc.getId()) : null)
                .externalId(doc.getExternalId())
                .name(doc.getName())
                .address(doc.getAddress())
                .latitude(doc.getLocation() != null ? doc.getLocation().getLat() : 0)
                .longitude(doc.getLocation() != null ? doc.getLocation().getLon() : 0)
                .totalCount(doc.getTotalCount())
                .availableCount(doc.getAvailableCount())
                .distance(distance != null ? Math.round(distance * 100.0) / 100.0 : null)
                .build();
    }

    /**
     * 위치 기반 주차장 검색 (MySQL)
     */
    public List<ParkingResponse> searchByMysql(ParkingSearchRequest request) {
        log.info("[MySQL] Parking search: lat={}, lng={}, radius={}km",
                request.getLat(), request.getLng(), request.getRadius());

        long startTime = System.currentTimeMillis();

        // Bounding box 계산 (성능 최적화)
        double[] bbox = GeoUtils.getBoundingBox(
                request.getLat(), request.getLng(), request.getRadius());

        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize());

        List<Facility> facilities;
        if (Boolean.TRUE.equals(request.getAvailable())) {
            facilities = facilityRepository.findAvailableByLocationWithinRadius(
                    request.getLat(), request.getLng(), request.getRadius(),
                    FacilityType.PARKING.name(),
                    bbox[0], bbox[1], bbox[2], bbox[3],
                    pageable);
        } else {
            facilities = facilityRepository.findByLocationWithinRadius(
                    request.getLat(), request.getLng(), request.getRadius(),
                    FacilityType.PARKING.name(),
                    bbox[0], bbox[1], bbox[2], bbox[3],
                    pageable);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[MySQL] Search completed: {} results in {}ms", facilities.size(), elapsed);

        return facilities.stream()
                .map(f -> toResponseFromFacility(f, request.getLat(), request.getLng()))
                .toList();
    }

    private ParkingResponse toResponseFromFacility(Facility facility, double lat, double lng) {
        double distance = GeoUtils.calculateDistance(
                lat, lng,
                facility.getLocation().getLatitude(),
                facility.getLocation().getLongitude());

        return ParkingResponse.builder()
                .id(facility.getId())
                .externalId(facility.getExternalId())
                .name(facility.getName())
                .address(facility.getAddress())
                .latitude(facility.getLocation().getLatitude())
                .longitude(facility.getLocation().getLongitude())
                .totalCount(facility.getAvailability().getTotalCount())
                .availableCount(facility.getAvailability().getAvailableCount())
                .distance(Math.round(distance * 100.0) / 100.0)
                .build();
    }
}
