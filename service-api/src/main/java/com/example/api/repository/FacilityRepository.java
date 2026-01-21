package com.example.api.repository;

import com.example.common.domain.FacilityType;
import com.example.common.domain.entity.Facility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    Optional<Facility> findByExternalId(String externalId);

    List<Facility> findByType(FacilityType type);

    List<Facility> findByTypeAndAvailabilityAvailableCountGreaterThan(FacilityType type, int count);

    long countByType(FacilityType type);

    /**
     * 위치 기반 주차장 검색 (Haversine formula)
     * 거리 계산 후 정렬하여 반환
     */
    @Query(value = """
        SELECT f.*,
               (6371 * acos(cos(radians(:lat)) * cos(radians(f.latitude))
               * cos(radians(f.longitude) - radians(:lng))
               + sin(radians(:lat)) * sin(radians(f.latitude)))) AS distance
        FROM facility f
        WHERE f.type = :type
          AND f.latitude BETWEEN :minLat AND :maxLat
          AND f.longitude BETWEEN :minLng AND :maxLng
          AND (6371 * acos(cos(radians(:lat)) * cos(radians(f.latitude))
               * cos(radians(f.longitude) - radians(:lng))
               + sin(radians(:lat)) * sin(radians(f.latitude)))) <= :radius
        ORDER BY distance
        """, nativeQuery = true)
    List<Facility> findByLocationWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radiusKm,
            @Param("type") String type,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            Pageable pageable);

    /**
     * 위치 기반 + 가용 필터 검색
     */
    @Query(value = """
        SELECT f.*,
               (6371 * acos(cos(radians(:lat)) * cos(radians(f.latitude))
               * cos(radians(f.longitude) - radians(:lng))
               + sin(radians(:lat)) * sin(radians(f.latitude)))) AS distance
        FROM facility f
        WHERE f.type = :type
          AND f.latitude BETWEEN :minLat AND :maxLat
          AND f.longitude BETWEEN :minLng AND :maxLng
          AND f.available_count > 0
          AND (6371 * acos(cos(radians(:lat)) * cos(radians(f.latitude))
               * cos(radians(f.longitude) - radians(:lng))
               + sin(radians(:lat)) * sin(radians(f.latitude)))) <= :radius
        ORDER BY distance
        """, nativeQuery = true)
    List<Facility> findAvailableByLocationWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radiusKm,
            @Param("type") String type,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            Pageable pageable);

    /**
     * 테스트 데이터 삭제 (externalId prefix 기준)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Facility f WHERE f.externalId LIKE :prefix%")
    long deleteByExternalIdStartingWith(@Param("prefix") String prefix);
}
