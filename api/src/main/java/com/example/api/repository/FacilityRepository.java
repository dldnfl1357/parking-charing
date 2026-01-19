package com.example.api.repository;

import com.example.common.domain.FacilityType;
import com.example.common.domain.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    Optional<Facility> findByExternalId(String externalId);

    List<Facility> findByType(FacilityType type);

    List<Facility> findByTypeAndAvailabilityAvailableCountGreaterThan(FacilityType type, int count);

    long countByType(FacilityType type);
}
