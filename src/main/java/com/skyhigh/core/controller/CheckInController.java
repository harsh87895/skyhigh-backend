package com.skyhigh.core.controller;

import com.skyhigh.core.dto.CheckInRequest;
import com.skyhigh.core.dto.CheckInResponse;
import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for check-in operations
 */
@RestController
@RequestMapping("/api/v1/check-in")
@Tag(name = "Check-In", description = "Check-in management APIs")
@Slf4j
@RequiredArgsConstructor
@Validated
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * Start a new check-in session
     */
    @PostMapping("/start")
    @Operation(summary = "Start check-in", description = "Initiate a new check-in session for a passenger")
    public ResponseEntity<CheckInResponse> startCheckIn(@Valid @RequestBody CheckInRequest request) {
        log.info("Starting check-in for passenger {} on flight {}",
                 request.getPassengerId(), request.getFlightId());

        CheckIn checkIn = checkInService.startCheckIn(
            request.getPassengerId(),
            request.getFlightId(),
            request.getBookingReference()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CheckInResponse.from(checkIn));
    }

    /**
     * Get check-in details
     */
    @GetMapping("/{checkInId}")
    @Operation(summary = "Get check-in details", description = "Retrieve check-in information by ID")
    public ResponseEntity<CheckInResponse> getCheckIn(@PathVariable UUID checkInId) {
        log.debug("Getting check-in details for {}", checkInId);

        CheckIn checkIn = checkInService.getCheckIn(checkInId);
        return ResponseEntity.ok(CheckInResponse.from(checkIn));
    }

    /**
     * Complete check-in process
     */
    @PostMapping("/{checkInId}/complete")
    @Operation(summary = "Complete check-in", description = "Finalize check-in and generate boarding pass")
    public ResponseEntity<CheckInResponse> completeCheckIn(@PathVariable UUID checkInId) {
        log.info("Completing check-in {}", checkInId);

        CheckIn checkIn = checkInService.completeCheckIn(checkInId);
        return ResponseEntity.ok(CheckInResponse.from(checkIn));
    }

    /**
     * Cancel check-in
     */
    @DeleteMapping("/{checkInId}")
    @Operation(summary = "Cancel check-in", description = "Cancel an in-progress check-in")
    public ResponseEntity<Void> cancelCheckIn(@PathVariable UUID checkInId) {
        log.info("Canceling check-in {}", checkInId);

        checkInService.cancelCheckIn(checkInId);
        return ResponseEntity.noContent().build();
    }
}


