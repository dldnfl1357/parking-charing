package com.example.api.document;

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
 * ElasticSearch 충전소 문서
 */
@Document(indexName = "facilities")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargerDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String externalId;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String address;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Integer)
    private int totalCount;

    @Field(type = FieldType.Integer)
    private int availableCount;

    @Field(type = FieldType.Text)
    private String extraInfo;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS || uuuu-MM-dd")
    private LocalDateTime collectedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS || uuuu-MM-dd")
    private LocalDateTime updatedAt;
}
