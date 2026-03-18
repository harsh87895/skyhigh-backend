package com.skyhigh.core.service;

import com.skyhigh.core.model.entity.Seat;
import com.skyhigh.core.model.enums.SeatClass;
import com.skyhigh.core.model.enums.SeatPosition;
import com.skyhigh.core.model.enums.SeatStatus;
import com.skyhigh.core.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for seat queries and seat map generation
 * Implements caching for high-performance access
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;

    /**
     * Get seat map for a flight with Redis caching (2-second TTL)
     * Seats in HELD or CONFIRMED status are shown as UNAVAILABLE to other passengers
     */
    @Cacheable(value = "seatMap", key = "#flightId")
    @Transactional(readOnly = true)
    public List<Seat> getSeatMap(String flightId) {
        log.debug("Loading seat map from database for flight {} (cache miss)", flightId);
        return seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc(flightId);
    }

    /**
     * Get available seats only for a flight
     */
    @Transactional(readOnly = true)
    public List<Seat> getAvailableSeats(String flightId) {
        List<Seat> allSeats = getSeatMap(flightId);
        return allSeats.stream()
            .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
            .collect(Collectors.toList());
    }

    /**
     * Get seats filtered by class and position
     */
    @Transactional(readOnly = true)
    public List<Seat> getFilteredSeats(String flightId, SeatClass seatClass, SeatPosition position) {
        List<Seat> seats = getSeatMap(flightId);

        // Apply filters
        if (seatClass != null) {
            seats = seats.stream()
                .filter(s -> s.getSeatClass() == seatClass)
                .collect(Collectors.toList());
        }

        if (position != null) {
            seats = seats.stream()
                .filter(s -> s.getPosition() == position)
                .collect(Collectors.toList());
        }

        return seats;
    }

    /**
     * Get count of available seats
     */
    @Transactional(readOnly = true)
    public long getAvailableSeatsCount(String flightId) {
        return seatRepository.countByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE);
    }

    /**
     * Check if a specific seat is available
     */
    @Transactional(readOnly = true)
    public boolean isSeatAvailable(String flightId, String seatNumber) {
        List<Seat> seats = getSeatMap(flightId);
        return seats.stream()
            .filter(s -> s.getSeatNumber().equals(seatNumber))
            .findFirst()
            .map(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
            .orElse(false);
    }
}

