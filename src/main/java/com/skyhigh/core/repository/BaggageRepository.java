package com.skyhigh.core.repository;

import com.skyhigh.core.model.entity.Baggage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Baggage entity
 */
@Repository
public interface BaggageRepository extends JpaRepository<Baggage, UUID> {

    /**
     * Find baggage by check-in ID
     */
    Optional<Baggage> findByCheckInId(UUID checkInId);
}
