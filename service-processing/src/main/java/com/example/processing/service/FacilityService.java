package com.example.processing.service;

import com.example.common.domain.FacilityType;
import com.example.common.domain.entity.Facility;
import com.example.common.event.FacilityEvent;
import com.example.processing.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 시설 저장 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;

    /**
     * 시설 정보 저장/갱신 (전체 업데이트)
     */
    @Transactional
    public Facility saveOrUpdate(FacilityEvent event) {
        Optional<Facility> existingOpt = facilityRepository.findByExternalId(event.getExternalId());

        Facility facility;
        if (existingOpt.isPresent()) {
            facility = existingOpt.get();
            facility.updateFrom(
                    event.getName(),
                    event.getAddress(),
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getTotalCount(),
                    event.getAvailableCount(),
                    event.getExtraInfo(),
                    event.getCollectedAt()
            );
            log.debug("Updated facility: {}", event.getExternalId());
        } else {
            facility = createFacility(event);
            log.debug("Created facility: {}", event.getExternalId());
        }

        return facilityRepository.save(facility);
    }

    /**
     * 시설 상태만 업데이트 (부분 업데이트)
     * - availableCount만 변경
     * - 존재하지 않는 시설은 무시 (메타 정보 동기화 시 생성됨)
     */
    @Transactional
    public Optional<Facility> updateStatus(FacilityEvent event) {
        Optional<Facility> existingOpt = facilityRepository.findByExternalId(event.getExternalId());

        if (existingOpt.isEmpty()) {
            log.debug("Facility not found for status update, skipping: {}", event.getExternalId());
            return Optional.empty();
        }

        Facility facility = existingOpt.get();
        facility.updateAvailability(facility.getAvailability().getTotalCount(), event.getAvailableCount());

        log.debug("Status updated: {} -> available={}", event.getExternalId(), event.getAvailableCount());
        return Optional.of(facilityRepository.save(facility));
    }

    private Facility createFacility(FacilityEvent event) {
        if (event.getType() == FacilityType.PARKING) {
            return Facility.createParking(
                    event.getExternalId(),
                    event.getName(),
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getAddress(),
                    event.getTotalCount(),
                    event.getAvailableCount(),
                    event.getExtraInfo()
            );
        } else {
            return Facility.createCharging(
                    event.getExternalId(),
                    event.getName(),
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getAddress(),
                    event.getTotalCount(),
                    event.getAvailableCount(),
                    event.getExtraInfo()
            );
        }
    }
}
