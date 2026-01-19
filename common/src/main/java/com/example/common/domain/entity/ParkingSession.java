package com.example.common.domain.entity;

import com.example.common.domain.vo.Money;
import com.example.common.domain.vo.ParkingFee;
import com.example.common.domain.vo.VehicleNumber;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 주차 세션 엔티티
 */
@Entity
@Table(
        name = "parking_session",
        indexes = {
                @Index(name = "idx_session_facility", columnList = "facility_id"),
                @Index(name = "idx_session_vehicle", columnList = "vehicle_number"),
                @Index(name = "idx_session_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "facility_id", nullable = false)
    private Long facilityId;

    @Embedded
    private VehicleNumber vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParkingStatus status;

    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "fee_amount"))
    private Money fee;

    @Embedded
    private ParkingFee feePolicy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ParkingStatus {
        ACTIVE,     // 주차 중
        COMPLETED,  // 출차 완료
        CANCELLED   // 취소
    }

    // === 팩토리 메서드 ===

    public static ParkingSession createEntry(Long facilityId, VehicleNumber vehicleNumber, ParkingFee feePolicy) {
        ParkingSession session = new ParkingSession();
        session.facilityId = facilityId;
        session.vehicleNumber = vehicleNumber;
        session.status = ParkingStatus.ACTIVE;
        session.entryTime = LocalDateTime.now();
        session.feePolicy = feePolicy;
        session.fee = Money.zero();
        session.createdAt = LocalDateTime.now();
        session.updatedAt = LocalDateTime.now();
        return session;
    }

    // === 도메인 로직 ===

    /**
     * 출차 처리
     */
    public void exit() {
        if (this.status != ParkingStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태의 주차 세션만 출차 처리할 수 있습니다");
        }
        this.exitTime = LocalDateTime.now();
        this.fee = calculateFee();
        this.status = ParkingStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 요금 계산
     */
    public Money calculateFee() {
        if (feePolicy == null) {
            return Money.zero();
        }
        LocalDateTime endTime = exitTime != null ? exitTime : LocalDateTime.now();
        long minutes = Duration.between(entryTime, endTime).toMinutes();
        int feeAmount = feePolicy.calculate((int) minutes);
        return Money.of(feeAmount);
    }

    /**
     * 주차 시간 (분)
     */
    public long getParkingMinutes() {
        LocalDateTime endTime = exitTime != null ? exitTime : LocalDateTime.now();
        return Duration.between(entryTime, endTime).toMinutes();
    }

    /**
     * 주차 시간 문자열
     */
    public String getParkingDurationString() {
        long minutes = getParkingMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (hours > 0) {
            return String.format("%d시간 %d분", hours, mins);
        }
        return String.format("%d분", mins);
    }

    /**
     * 주차 중 여부
     */
    public boolean isActive() {
        return status == ParkingStatus.ACTIVE;
    }

    /**
     * 취소
     */
    public void cancel() {
        if (this.status != ParkingStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태의 주차 세션만 취소할 수 있습니다");
        }
        this.status = ParkingStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
}
