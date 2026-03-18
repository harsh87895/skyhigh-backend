package com.skyhigh.core.service;

import com.skyhigh.core.exception.CheckInAlreadyExistsException;
import com.skyhigh.core.exception.CheckInNotFoundException;
import com.skyhigh.core.exception.FlightNotFoundException;
import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.entity.Flight;
import com.skyhigh.core.model.entity.Passenger;
import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.model.enums.CheckInStatus;
import com.skyhigh.core.repository.CheckInRepository;
import com.skyhigh.core.repository.FlightRepository;
import com.skyhigh.core.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing check-in workflow
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final FlightRepository flightRepository;
    private final PassengerRepository passengerRepository;
    private final SeatHoldService seatHoldService;

    /**
     * Start a new check-in session
     */
    @Transactional
    public CheckIn startCheckIn(String passengerId, String flightId, String bookingReference) {
        log.info("Starting check-in for passenger {} on flight {}", passengerId, flightId);

        // Validate flight exists
        Flight flight = flightRepository.findById(flightId)
            .orElseThrow(() -> new FlightNotFoundException(flightId));

        // Validate passenger exists (in real system, would verify booking)
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElse(null);

        if (passenger == null) {
            log.warn("Passenger {} not found, this would normally be validated against booking", passengerId);
        }

        // Check if passenger already has an active check-in
        if (checkInRepository.existsByPassengerIdAndFlightId(passengerId, flightId)) {
            throw new CheckInAlreadyExistsException(passengerId, flightId);
        }

        // Create new check-in
        CheckIn checkIn = CheckIn.builder()
            .passengerId(passengerId)
            .flightId(flightId)
            .bookingReference(bookingReference)
            .status(CheckInStatus.IN_PROGRESS)
            .paymentRequired(false)
            .build();

        checkIn = checkInRepository.save(checkIn);

        log.info("Check-in {} created for passenger {} on flight {}",
                 checkIn.getCheckInId(), passengerId, flightId);

        return checkIn;
    }

    /**
     * Get check-in details
     */
    @Transactional(readOnly = true)
    public CheckIn getCheckIn(UUID checkInId) {
        return checkInRepository.findById(checkInId)
            .orElseThrow(() -> new CheckInNotFoundException(checkInId));
    }

    /**
     * Complete check-in process
     */
    @Transactional
    public CheckIn completeCheckIn(UUID checkInId) {
        log.info("Completing check-in {}", checkInId);

        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new CheckInNotFoundException(checkInId));

        // Validate check-in is in valid state
        if (checkIn.getStatus() == CheckInStatus.COMPLETED) {
            log.warn("Check-in {} is already completed", checkInId);
            return checkIn;
        }

        if (checkIn.getStatus() == CheckInStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Cannot complete check-in while awaiting payment");
        }

        // Validate seat is confirmed
        if (checkIn.getSeatNumber() == null) {
            throw new IllegalStateException("No seat assigned to check-in");
        }

        // Verify hold is confirmed
        List<SeatHold> holds = seatHoldService.getActiveHolds(checkInId);
        if (!holds.isEmpty()) {
            throw new IllegalStateException("Seat hold is still active. Please confirm seat first.");
        }

        // Mark check-in as completed
        checkIn.setStatus(CheckInStatus.COMPLETED);
        checkIn.setCompletedAt(Instant.now());
        checkIn = checkInRepository.save(checkIn);

        log.info("Check-in {} completed successfully. Seat: {}", checkInId, checkIn.getSeatNumber());

        return checkIn;
    }

    /**
     * Cancel check-in
     */
    @Transactional
    public void cancelCheckIn(UUID checkInId) {
        log.info("Canceling check-in {}", checkInId);

        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new CheckInNotFoundException(checkInId));

        // Release any active holds
        List<SeatHold> holds = seatHoldService.getActiveHolds(checkInId);
        for (SeatHold hold : holds) {
            seatHoldService.releaseHold(hold);
        }

        checkIn.setStatus(CheckInStatus.EXPIRED);
        checkInRepository.save(checkIn);

        log.info("Check-in {} canceled", checkInId);
    }

    /**
     * Update check-in status
     */
    @Transactional
    public CheckIn updateCheckInStatus(UUID checkInId, CheckInStatus newStatus) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new CheckInNotFoundException(checkInId));

        log.info("Updating check-in {} status from {} to {}",
                 checkInId, checkIn.getStatus(), newStatus);

        checkIn.setStatus(newStatus);
        return checkInRepository.save(checkIn);
    }
}


