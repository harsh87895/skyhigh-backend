package com.skyhigh.core.repository;

import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.enums.CheckInStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CheckIn entity
 */
@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    /**
     * Find check-in by passenger and flight
     */
    Optional<CheckIn> findByPassengerIdAndFlightId(String passengerId, String flightId);

    /**
     * Check if check-in exists for passenger and flight
     */
    boolean existsByPassengerIdAndFlightId(String passengerId, String flightId);

    /**
     * Find check-in by status
     */
    Optional<CheckIn> findByCheckInIdAndStatus(UUID checkInId, CheckInStatus status);
}
