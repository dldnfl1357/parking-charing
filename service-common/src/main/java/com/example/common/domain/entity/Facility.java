package com.example.common.domain.entity;

import com.example.common.domain.FacilityType;
import com.example.common.domain.vo.Availability;
import com.example.common.domain.vo.Location;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 시설 엔티티 (Aggregate Root)
 * <p>
 * 주차장과 충전소의 공통 정보를 담는 핵심 도메인 엔티티입니다.
 * 값 객체(Location, Availability)를 통해 도메인 로직을 캡슐화합니다.
 */
@Entity
@Table(
        name = "facility",
        indexes = {
                @Index(name = "idx_facility_type", columnList = "type"),
                @Index(name = "idx_facility_location", columnList = "latitude, longitude")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_external_id", columnNames = "external_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FacilityType type;

    @Column(nullable = false, length = 200)
    private String name;

    @Embedded
    private Location location;

    @Column(length = 500)
    private String address;

    @Embedded
    private Availability availability;

    @Column(name = "extra_info", columnDefinition = "JSON")
    private String extraInfo;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // === 생성자 ===

    private Facility(String externalId, FacilityType type, String name,
                     Location location, String address, Availability availability,
                     String extraInfo, LocalDateTime collectedAt) {
        this.externalId = externalId;
        this.type = type;
        this.name = name;
        this.location = location;
        this.address = address;
        this.availability = availability != null ? availability : new Availability(0, 0);
        this.extraInfo = extraInfo;
        this.collectedAt = collectedAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // === 팩토리 메서드 ===

    /**
     * 주차장 생성
     */
    public static Facility createParking(String externalId, String name,
                                          double latitude, double longitude,
                                          String address, int totalCount, int availableCount,
                                          String extraInfo) {
        return new Facility(
                externalId,
                FacilityType.PARKING,
                name,
                new Location(latitude, longitude),
                address,
                new Availability(totalCount, Math.min(availableCount, totalCount)),
                extraInfo,
                LocalDateTime.now()
        );
    }

    /**
     * 충전소 생성
     */
    public static Facility createCharging(String externalId, String name,
                                           double latitude, double longitude,
                                           String address, int totalCount, int availableCount,
                                           String extraInfo) {
        return new Facility(
                externalId,
                FacilityType.CHARGING,
                name,
                new Location(latitude, longitude),
                address,
                new Availability(totalCount, Math.min(availableCount, totalCount)),
                extraInfo,
                LocalDateTime.now()
        );
    }

    // === 도메인 로직 ===

    /**
     * 가용성 갱신
     */
    public void updateAvailability(int totalCount, int availableCount) {
        this.availability = new Availability(totalCount, Math.min(availableCount, totalCount));
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 위치 갱신
     */
    public void updateLocation(double latitude, double longitude) {
        this.location = new Location(latitude, longitude);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 전체 정보 갱신
     */
    public void updateFrom(String name, String address, double latitude, double longitude,
                           int totalCount, int availableCount, String extraInfo,
                           LocalDateTime collectedAt) {
        this.name = name;
        this.address = address;
        this.location = new Location(latitude, longitude);
        this.availability = new Availability(totalCount, Math.min(availableCount, totalCount));
        this.extraInfo = extraInfo;
        this.collectedAt = collectedAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 점유율 (0.0 ~ 1.0)
     */
    public double getOccupancyRate() {
        return availability.getOccupancyRate();
    }

    /**
     * 만차 여부
     */
    public boolean isFull() {
        return availability.isFull();
    }

    /**
     * 혼잡도 레벨
     */
    public Availability.CongestionLevel getCongestionLevel() {
        return availability.getCongestionLevel();
    }

    /**
     * 특정 위치로부터의 거리 (km)
     */
    public double distanceFrom(double fromLat, double fromLng) {
        return location.distanceTo(new Location(fromLat, fromLng));
    }

    /**
     * 주차장 여부
     */
    public boolean isParking() {
        return type == FacilityType.PARKING;
    }

    /**
     * 충전소 여부
     */
    public boolean isCharging() {
        return type == FacilityType.CHARGING;
    }

    // === equals & hashCode ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Facility facility = (Facility) o;
        return Objects.equals(externalId, facility.externalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalId);
    }

    @Override
    public String toString() {
        return String.format("Facility(id=%d, type=%s, name=%s)", id, type, name);
    }
}
