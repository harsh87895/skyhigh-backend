package com.skyhigh.core.controller;

import com.skyhigh.core.dto.SeatMapResponse;
import com.skyhigh.core.model.entity.Seat;
import com.skyhigh.core.model.enums.SeatClass;
import com.skyhigh.core.model.enums.SeatPosition;
import com.skyhigh.core.service.SeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for seat map browsing
 */
@RestController
@RequestMapping("/api/v1/flights")
@Tag(name = "Flights", description = "Flight and seat map APIs")
@Slf4j
@RequiredArgsConstructor
public class FlightController {

    private final SeatService seatService;

    /**
     * Get seat map for a flight (with Redis caching)
     */
    @GetMapping("/{flightId}/seats")
    @Operation(summary = "Get seat map", description = "Retrieve seat availability for a flight (cached for 2s)")
    public ResponseEntity<SeatMapResponse> getSeatMap(
            @PathVariable String flightId,
            @RequestParam(required = false) SeatClass seatClass,
            @RequestParam(required = false) SeatPosition position) {

        log.debug("Seat map request for flight {} (class={}, position={})",
                  flightId, seatClass, position);

        List<Seat> seats;

        if (seatClass != null || position != null) {
            seats = seatService.getFilteredSeats(flightId, seatClass, position);
        } else {
            seats = seatService.getSeatMap(flightId);
        }

        SeatMapResponse response = SeatMapResponse.from(flightId, seats);

        return ResponseEntity.ok(response);
    }

    /**
     * Get available seats count
     */
    @GetMapping("/{flightId}/seats/available/count")
    @Operation(summary = "Count available seats", description = "Get the number of available seats")
    public ResponseEntity<Long> getAvailableSeatsCount(@PathVariable String flightId) {
        log.debug("Available seats count request for flight {}", flightId);

        long count = seatService.getAvailableSeatsCount(flightId);
        return ResponseEntity.ok(count);
    }
}

