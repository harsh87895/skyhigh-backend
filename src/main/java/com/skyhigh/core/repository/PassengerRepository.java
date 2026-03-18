package com.skyhigh.core.repository;

import com.skyhigh.core.model.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Passenger entity
 */
@Repository
public interface PassengerRepository extends JpaRepository<Passenger, String> {
}
