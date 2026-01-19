package com.example.processing.service;

import com.example.common.domain.entity.Facility;
import com.example.processing.document.FacilityDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

/**
 * ElasticSearch 인덱싱 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 시설 문서 인덱싱
     */
    public void index(Facility facility) {
        try {
            FacilityDocument document = FacilityDocument.from(facility);
            elasticsearchOperations.save(document);
            log.debug("Indexed facility: {}", facility.getExternalId());
        } catch (Exception e) {
            log.error("Failed to index facility {}: {}", facility.getExternalId(), e.getMessage());
        }
    }

    /**
     * 시설 문서 삭제
     */
    public void delete(String facilityId) {
        try {
            elasticsearchOperations.delete(facilityId, FacilityDocument.class);
            log.debug("Deleted facility from index: {}", facilityId);
        } catch (Exception e) {
            log.error("Failed to delete facility {} from index: {}", facilityId, e.getMessage());
        }
    }
}
