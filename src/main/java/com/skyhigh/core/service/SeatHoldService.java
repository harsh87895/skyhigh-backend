package com.skyhigh.core.service;

import com.skyhigh.core.exception.*;
import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.entity.Seat;
import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.model.enums.CheckInStatus;
import com.skyhigh.core.model.enums.HoldStatus;
import com.skyhigh.core.model.enums.SeatStatus;
import com.skyhigh.core.repository.CheckInRepository;
import com.skyhigh.core.repository.SeatHoldRepository;
import com.skyhigh.core.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing seat holds with time-bound reservations
 * Implements pessimistic locking for conflict-free seat assignment
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeatHoldService {

    private final SeatRepository seatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final CheckInRepository checkInRepository;

    @Value("${app.seat-hold.duration-seconds:120}")
    private int holdDurationSeconds;

    /**
     * Hold a seat for a passenger with pessimistic locking
     * Only one passenger can hold the same seat at a time
     */
    @Transactional
    @CacheEvict(value = "seatMap", key = "#flightId")
    public SeatHold holdSeat(UUID checkInId, String flightId, String seatNumber) {
        log.info("Attempting to hold seat {} for check-in {}", seatNumber, checkInId);

        // 1. Validate check-in exists and is active
        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new CheckInNotFoundException(checkInId));

        if (checkIn.getStatus() != CheckInStatus.IN_PROGRESS) {
            throw new InvalidCheckInStateException("Check-in is not in progress. Current status: " + checkIn.getStatus());
        }

        // 2. Find and lock the seat (PESSIMISTIC_WRITE for absolute guarantee)
        Seat seat = seatRepository.findByFlightIdAndSeatNumberWithLock(flightId, seatNumber)
            .orElseThrow(() -> new SeatNotFoundException(seatNumber));

        // 3. Check if seat is available
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            log.warn("Seat {} is not available. Current status: {}", seatNumber, seat.getStatus());
            throw new SeatUnavailableException(seatNumber);
        }

        // 4. Release any previous hold by the same passenger
        seatHoldRepository.findByCheckInIdAndStatus(checkInId, HoldStatus.ACTIVE)
            .stream()
            .findFirst()
            .ifPresent(existingHold -> {
                log.info("Releasing previous hold {} for check-in {}", existingHold.getHoldId(), checkInId);
                releaseHold(existingHold);
            });

        // 5. Update seat status to HELD
        seat.setStatus(SeatStatus.HELD);
        seat = seatRepository.save(seat);

        // 6. Create hold record with expiration time
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(holdDurationSeconds);

        SeatHold hold = SeatHold.builder()
            .seatId(seat.getSeatId())
            .checkInId(checkInId)
            .status(HoldStatus.ACTIVE)
            .heldAt(now)
            .expiresAt(expiresAt)
            .build();

        hold = seatHoldRepository.save(hold);

        log.info("Seat {} successfully held for check-in {} until {}",
                 seatNumber, checkInId, expiresAt);

        return hold;
    }

    /**
     * Confirm a seat hold and permanently assign the seat
     */
    @Transactional
    @CacheEvict(value = "seatMap", allEntries = true)
    public void confirmHold(UUID holdId, UUID checkInId) {
        log.info("Confirming hold {} for check-in {}", holdId, checkInId);

        SeatHold hold = seatHoldRepository.findById(holdId)
            .orElseThrow(() -> new HoldNotFoundException(holdId));

        // Validate hold belongs to the check-in
        if (!hold.getCheckInId().equals(checkInId)) {
            throw new InvalidHoldStateException("Hold does not belong to this check-in");
        }

        // Validate hold is active
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new InvalidHoldStateException("Hold is not active. Current status: " + hold.getStatus());
        }

        // Validate hold has not expired
        if (hold.isExpired()) {
            log.warn("Hold {} has expired", holdId);
            throw new HoldExpiredException(holdId);
        }

        // Update hold status
        hold.setStatus(HoldStatus.CONFIRMED);
        hold.setConfirmedAt(Instant.now());
        seatHoldRepository.save(hold);

        // Update seat status to CONFIRMED (permanent)
        Seat seat = seatRepository.findById(hold.getSeatId())
            .orElseThrow(() -> new SeatNotFoundException(hold.getSeatId()));

        seat.setStatus(SeatStatus.CONFIRMED);
        seatRepository.save(seat);

        // Update check-in with seat number
        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new CheckInNotFoundException(checkInId));

        checkIn.setSeatNumber(seat.getSeatNumber());
        checkInRepository.save(checkIn);

        log.info("Hold {} confirmed. Seat {} permanently assigned to check-in {}",
                 holdId, seat.getSeatNumber(), checkInId);
    }

    /**
     * Release a seat hold manually
     */
    @Transactional
    @CacheEvict(value = "seatMap", allEntries = true)
    public void releaseHold(SeatHold hold) {
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            return;  // Already released
        }

        log.info("Releasing hold {}", hold.getHoldId());

        // Update hold status
        hold.setStatus(HoldStatus.RELEASED);
        seatHoldRepository.save(hold);

        // Release seat back to AVAILABLE
        Seat seat = seatRepository.findById(hold.getSeatId())
            .orElseThrow(() -> new SeatNotFoundException(hold.getSeatId()));

        if (seat.getStatus() == SeatStatus.HELD) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);

            log.info("Seat {} released back to AVAILABLE", seat.getSeatNumber());
        }
    }

    /**
     * Find all expired holds (used by scheduled job)
     */
    public List<SeatHold> findExpiredHolds() {
        return seatHoldRepository.findExpiredHolds(HoldStatus.ACTIVE, Instant.now());
    }

    /**
     * Get active holds for a check-in
     */
    public List<SeatHold> getActiveHolds(UUID checkInId) {
        return seatHoldRepository.findByCheckInIdAndStatus(checkInId, HoldStatus.ACTIVE);
    }

    /**
     * Check if a hold is still valid
     */
    public boolean isHoldValid(UUID holdId) {
        return seatHoldRepository.findById(holdId)
            .map(hold -> hold.getStatus() == HoldStatus.ACTIVE && !hold.isExpired())
            .orElse(false);
    }
}


