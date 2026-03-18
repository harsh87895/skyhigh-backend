package com.skyhigh.core.repository;

import com.skyhigh.core.model.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Flight entity
 */
@Repository
public interface FlightRepository extends JpaRepository<Flight, String> {
}
