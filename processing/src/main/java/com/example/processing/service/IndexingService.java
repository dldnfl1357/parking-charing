package com.example.processing.service;

import com.example.common.domain.entity.Facility;
import com.example.processing.document.FacilityDocument;
import com.example.processing.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ElasticSearch 인덱싱 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final FacilityRepository facilityRepository;

    private static final String INDEX_NAME = "facilities";

    /**
     * 시설 문서 인덱싱 (전체)
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
     * 시설 상태만 업데이트 (부분 업데이트)
     * - availableCount, updatedAt만 변경
     */
    public void updateStatus(Facility facility) {
        try {
            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("availableCount", facility.getAvailability().getAvailableCount());
            updateFields.put("updatedAt", LocalDateTime.now());

            UpdateQuery updateQuery = UpdateQuery.builder(facility.getId().toString())
                    .withDocument(Document.from(updateFields))
                    .build();

            elasticsearchOperations.update(updateQuery, IndexCoordinates.of(INDEX_NAME));
            log.debug("Updated status in ES: {} -> available={}",
                    facility.getExternalId(), facility.getAvailability().getAvailableCount());
        } catch (Exception e) {
            log.error("Failed to update status in ES {}: {}", facility.getExternalId(), e.getMessage());
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

    /**
     * 전체 재인덱싱 - DB의 모든 데이터를 ES에 인덱싱
     */
    public int reindexAll() {
        log.info("Starting full reindex from DB to ES");

        List<Facility> facilities = facilityRepository.findAll();
        log.info("Found {} facilities in DB", facilities.size());

        int success = 0;
        int failed = 0;

        for (Facility facility : facilities) {
            try {
                FacilityDocument document = FacilityDocument.from(facility);
                elasticsearchOperations.save(document);
                success++;
            } catch (Exception e) {
                log.error("Failed to index facility {}: {}", facility.getExternalId(), e.getMessage());
                failed++;
            }
        }

        log.info("Reindex completed: success={}, failed={}", success, failed);
        return success;
    }
}
