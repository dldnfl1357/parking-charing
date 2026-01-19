package com.example.api.repository;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.api.document.ParkingDocument;
import com.example.common.domain.FacilityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

/**
 * ElasticSearch 주차장 검색 리포지토리
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ParkingSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 위치 기반 주차장 검색
     */
    public SearchHits<ParkingDocument> search(
            Double lat, Double lng, double radiusKm,
            Boolean available, Boolean free,
            int page, int size) {

        log.info("Parking search request: lat={}, lng={}, radius={}km, available={}, free={}, page={}, size={}",
                lat, lng, radiusKm, available, free, page, size);

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 주차장 타입 필터
        boolBuilder.filter(Query.of(q -> q
                .term(t -> t.field("type").value(FacilityType.PARKING.name()))));

        // 거리 필터 (위치가 주어진 경우)
        if (lat != null && lng != null) {
            boolBuilder.filter(Query.of(q -> q
                    .geoDistance(g -> g
                            .field("location")
                            .distance(radiusKm + "km")
                            .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                            .distanceType(GeoDistanceType.Arc))));
        }

        // 주차 가능 필터
        if (available != null && available) {
            boolBuilder.filter(Query.of(q -> q
                    .range(r -> r.field("availableCount").gt(JsonData.of(0)))));
        }

        // 무료 주차장 필터 (extraInfo에서)
        if (free != null && free) {
            boolBuilder.filter(Query.of(q -> q
                    .match(m -> m.field("extraInfo").query("무료"))));
        }

        NativeQuery query;
        if (lat != null && lng != null) {
            final double finalLat = lat;
            final double finalLng = lng;
            query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                    .withSort(s -> s
                            .geoDistance(g -> g
                                    .field("location")
                                    .location(l -> l.latlon(ll -> ll.lat(finalLat).lon(finalLng)))
                                    .order(SortOrder.Asc)
                                    .unit(co.elastic.clients.elasticsearch._types.DistanceUnit.Kilometers)))
                    .withPageable(PageRequest.of(page, size))
                    .build();
        } else {
            query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                    .withPageable(PageRequest.of(page, size))
                    .build();
        }

        SearchHits<ParkingDocument> hits = elasticsearchOperations.search(query, ParkingDocument.class);
        log.info("Parking search result: totalHits={}, returned={}",
                hits.getTotalHits(), hits.getSearchHits().size());

        return hits;
    }
}
