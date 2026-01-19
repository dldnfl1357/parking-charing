package com.example.api.repository;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.api.document.ChargerDocument;
import com.example.common.domain.FacilityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ElasticSearch 충전소 검색 리포지토리
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChargerSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 위치 기반 충전소 검색
     */
    public SearchHits<ChargerDocument> search(
            Double lat, Double lng, double radiusKm,
            String chgerType, Boolean available, Boolean parkingFree,
            int page, int size) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 충전소 타입 필터
        boolBuilder.filter(Query.of(q -> q
                .term(t -> t.field("type").value(FacilityType.CHARGING.name()))));

        // 거리 필터 (위치가 주어진 경우)
        if (lat != null && lng != null) {
            boolBuilder.filter(Query.of(q -> q
                    .geoDistance(g -> g
                            .field("location")
                            .distance(radiusKm + "km")
                            .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                            .distanceType(GeoDistanceType.Arc))));
        }

        // 충전기 타입 필터 (extraInfo에서)
        if (chgerType != null) {
            boolBuilder.filter(Query.of(q -> q
                    .match(m -> m.field("extraInfo").query(chgerType))));
        }

        // 사용 가능 필터
        if (available != null && available) {
            boolBuilder.filter(Query.of(q -> q
                    .range(r -> r.field("availableCount").gt(JsonData.of(0)))));
        }

        // 주차 무료 필터 (extraInfo에서)
        if (parkingFree != null && parkingFree) {
            boolBuilder.filter(Query.of(q -> q
                    .match(m -> m.field("extraInfo").query("parkingFree\":\"Y"))));
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

        return elasticsearchOperations.search(query, ChargerDocument.class);
    }

    /**
     * 내 주변 충전소 (거리순)
     */
    public SearchHits<ChargerDocument> findNearby(double lat, double lng, double radiusKm, int limit) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 충전소 타입 필터
        boolBuilder.filter(Query.of(q -> q
                .term(t -> t.field("type").value(FacilityType.CHARGING.name()))));

        // 거리 필터
        boolBuilder.filter(Query.of(q -> q
                .geoDistance(g -> g
                        .field("location")
                        .distance(radiusKm + "km")
                        .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                        .distanceType(GeoDistanceType.Arc))));

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                .withSort(s -> s
                        .geoDistance(g -> g
                                .field("location")
                                .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                                .order(SortOrder.Asc)
                                .unit(co.elastic.clients.elasticsearch._types.DistanceUnit.Kilometers)))
                .withPageable(PageRequest.of(0, limit))
                .build();

        return elasticsearchOperations.search(query, ChargerDocument.class);
    }

    /**
     * 사용 가능한 충전소
     */
    public SearchHits<ChargerDocument> findAvailable(Double lat, Double lng, double radiusKm, int limit) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 충전소 타입 필터
        boolBuilder.filter(Query.of(q -> q
                .term(t -> t.field("type").value(FacilityType.CHARGING.name()))));

        // 사용 가능 필터
        boolBuilder.filter(Query.of(q -> q
                .range(r -> r.field("availableCount").gt(JsonData.of(0)))));

        // 거리 필터 (위치가 주어진 경우)
        if (lat != null && lng != null) {
            boolBuilder.filter(Query.of(q -> q
                    .geoDistance(g -> g
                            .field("location")
                            .distance(radiusKm + "km")
                            .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                            .distanceType(GeoDistanceType.Arc))));
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
                    .withPageable(PageRequest.of(0, limit))
                    .build();
        } else {
            query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                    .withPageable(PageRequest.of(0, limit))
                    .build();
        }

        return elasticsearchOperations.search(query, ChargerDocument.class);
    }

    /**
     * ID로 충전소 조회
     */
    public ChargerDocument findById(String id) {
        return elasticsearchOperations.get(id, ChargerDocument.class);
    }

    /**
     * 충전소 통계
     */
    public ChargerStats getStats() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q
                        .term(t -> t.field("type").value(FacilityType.CHARGING.name()))))
                .withMaxResults(0)
                .build();

        SearchHits<ChargerDocument> hits = elasticsearchOperations.search(query, ChargerDocument.class);

        ChargerStats stats = new ChargerStats();
        stats.totalCount = hits.getTotalHits();

        return stats;
    }

    public static class ChargerStats {
        public long totalCount;
        public long availableCount;
        public long unavailableCount;
        public Map<String, Long> byRegion = new LinkedHashMap<>();
    }
}
