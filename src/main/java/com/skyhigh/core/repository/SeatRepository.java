package com.skyhigh.core.repository;

import com.skyhigh.core.model.entity.Seat;
import com.skyhigh.core.model.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Seat entity with pessimistic locking for concurrency control
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    /**
     * Find all seats for a flight
     */
    List<Seat> findByFlightIdOrderByRowNumberAscColumnLetterAsc(String flightId);

    /**
     * Find seat by flight and seat number with pessimistic write lock
     * This prevents concurrent modifications to the same seat
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.flightId = :flightId AND s.seatNumber = :seatNumber")
    Optional<Seat> findByFlightIdAndSeatNumberWithLock(
        @Param("flightId") String flightId,
        @Param("seatNumber") String seatNumber
    );

    /**
     * Find seat by ID with pessimistic write lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.seatId = :seatId")
    Optional<Seat> findByIdWithLock(@Param("seatId") UUID seatId);

    /**
     * Count available seats for a flight
     */
    long countByFlightIdAndStatus(String flightId, SeatStatus status);
}
