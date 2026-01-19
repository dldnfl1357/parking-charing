package com.example.processing.document;

import com.example.common.domain.FacilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.time.LocalDateTime;

/**
 * ElasticSearch 시설 문서
 */
@Document(indexName = "facilities")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String externalId;

    @Field(type = FieldType.Keyword)
    private FacilityType type;

    @Field(type = FieldType.Text, analyzer = "korean")
    private String name;

    @Field(type = FieldType.Text, analyzer = "korean")
    private String address;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Integer)
    private int totalCount;

    @Field(type = FieldType.Integer)
    private int availableCount;

    @Field(type = FieldType.Double)
    private double occupancyRate;

    @Field(type = FieldType.Object)
    private String extraInfo;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS || uuuu-MM-dd")
    private LocalDateTime collectedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS || uuuu-MM-dd")
    private LocalDateTime updatedAt;

    /**
     * Facility 엔티티에서 변환
     */
    public static FacilityDocument from(com.example.common.domain.entity.Facility facility) {
        return FacilityDocument.builder()
                .id(facility.getId().toString())
                .externalId(facility.getExternalId())
                .type(facility.getType())
                .name(facility.getName())
                .address(facility.getAddress())
                .location(new GeoPoint(
                        facility.getLocation().getLatitude(),
                        facility.getLocation().getLongitude()))
                .totalCount(facility.getAvailability().getTotalCount())
                .availableCount(facility.getAvailability().getAvailableCount())
                .occupancyRate(facility.getOccupancyRate())
                .extraInfo(facility.getExtraInfo())
                .collectedAt(facility.getCollectedAt())
                .updatedAt(facility.getUpdatedAt())
                .build();
    }
}
