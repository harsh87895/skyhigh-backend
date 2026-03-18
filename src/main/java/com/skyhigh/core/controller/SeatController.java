package com.skyhigh.core.controller;

import com.skyhigh.core.dto.ConfirmSeatRequest;
import com.skyhigh.core.dto.ConfirmSeatResponse;
import com.skyhigh.core.dto.HoldSeatRequest;
import com.skyhigh.core.dto.HoldSeatResponse;
import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.repository.CheckInRepository;
import com.skyhigh.core.service.SeatHoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST Controller for seat operations (hold and confirm)
 */
@RestController
@RequestMapping("/api/v1/seats")
@Tag(name = "Seats", description = "Seat reservation and management APIs")
@Slf4j
@RequiredArgsConstructor
@Validated
public class SeatController {

    private final SeatHoldService seatHoldService;
    private final CheckInRepository checkInRepository;

    /**
     * Hold a seat (120-second reservation)
     */
    @PostMapping("/hold")
    @Operation(summary = "Hold a seat", description = "Reserve a seat for 120 seconds")
    public ResponseEntity<HoldSeatResponse> holdSeat(@Valid @RequestBody HoldSeatRequest request) {
        log.info("Hold request for seat {} on check-in {}",
                 request.getSeatNumber(), request.getCheckInId());

        SeatHold hold = seatHoldService.holdSeat(
            request.getCheckInId(),
            request.getFlightId(),
            request.getSeatNumber()
        );

        HoldSeatResponse response = HoldSeatResponse.from(hold, request.getSeatNumber());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /**
     * Confirm a seat hold (permanent assignment)
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirm seat", description = "Permanently assign a held seat")
    public ResponseEntity<ConfirmSeatResponse> confirmSeat(@Valid @RequestBody ConfirmSeatRequest request) {
        log.info("Confirm request for hold {}", request.getHoldId());

        seatHoldService.confirmHold(request.getHoldId(), request.getCheckInId());

        // Get seat number from check-in (it was set during confirmation)
        CheckIn checkIn = checkInRepository.findById(request.getCheckInId()).orElse(null);
        String seatNumber = checkIn != null ? checkIn.getSeatNumber() : "Unknown";

        ConfirmSeatResponse response = ConfirmSeatResponse.builder()
            .holdId(request.getHoldId())
            .seatNumber(seatNumber)
            .status("CONFIRMED")
            .confirmedAt(Instant.now())
            .message("Seat confirmed successfully")
            .build();

        return ResponseEntity.ok(response);
    }
}

