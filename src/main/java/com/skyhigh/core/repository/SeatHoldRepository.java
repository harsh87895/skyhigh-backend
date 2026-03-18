package com.skyhigh.core.repository;

import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.model.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SeatHold entity
 */
@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {

    /**
     * Find active hold by seat ID
     */
    Optional<SeatHold> findBySeatIdAndStatus(UUID seatId, HoldStatus status);

    /**
     * Find hold by check-in ID and seat ID
     */
    Optional<SeatHold> findByCheckInIdAndSeatId(UUID checkInId, UUID seatId);

    /**
     * Find all expired holds that need cleanup
     */
    @Query("SELECT h FROM SeatHold h WHERE h.status = :status AND h.expiresAt < :now")
    List<SeatHold> findExpiredHolds(
        @Param("status") HoldStatus status,
        @Param("now") Instant now
    );

    /**
     * Find all active holds for a check-in
     */
    List<SeatHold> findByCheckInIdAndStatus(UUID checkInId, HoldStatus status);
}
