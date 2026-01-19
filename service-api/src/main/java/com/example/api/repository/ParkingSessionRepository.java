package com.example.api.repository;

import com.example.common.domain.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    Optional<ParkingSession> findByIdAndStatus(Long id, ParkingSession.ParkingStatus status);

    List<ParkingSession> findByFacilityIdAndStatus(Long facilityId, ParkingSession.ParkingStatus status);

    Optional<ParkingSession> findByVehicleNumber_ValueAndStatus(String vehicleNumber, ParkingSession.ParkingStatus status);
}
