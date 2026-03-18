package com.skyhigh.core.controller;

import com.skyhigh.core.dto.BaggageRequest;
import com.skyhigh.core.dto.BaggageResponse;
import com.skyhigh.core.model.entity.Baggage;
import com.skyhigh.core.service.BaggageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST Controller for baggage validation
 */
@RestController
@RequestMapping("/api/v1/baggage")
@Tag(name = "Baggage", description = "Baggage validation and management APIs")
@Slf4j
@RequiredArgsConstructor
@Validated
public class BaggageController {

    private final BaggageService baggageService;

    @Value("${app.baggage.max-free-weight:25.0}")
    private BigDecimal maxFreeWeight;

    /**
     * Validate baggage weight and calculate fees
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate baggage", description = "Validate baggage weight and calculate excess fees")
    public ResponseEntity<BaggageResponse> validateBaggage(@Valid @RequestBody BaggageRequest request) {
        log.info("Validating baggage for check-in {}: weight={}kg, pieces={}",
                 request.getCheckInId(), request.getWeight(), request.getPieces());

        Baggage baggage = baggageService.validateBaggage(
            request.getCheckInId(),
            request.getWeight(),
            request.getPieces()
        );

        boolean paymentRequired = request.getWeight().compareTo(maxFreeWeight) > 0;

        BaggageResponse response = BaggageResponse.from(baggage, paymentRequired);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /**
     * Get baggage information
     */
    @GetMapping("/{checkInId}")
    @Operation(summary = "Get baggage details", description = "Retrieve baggage information for a check-in")
    public ResponseEntity<BaggageResponse> getBaggage(@PathVariable UUID checkInId) {
        log.debug("Getting baggage details for check-in {}", checkInId);

        Baggage baggage = baggageService.getBaggage(checkInId);
        boolean paymentRequired = baggage.getExcessFee() != null &&
                                 baggage.getExcessFee().compareTo(BigDecimal.ZERO) > 0;

        BaggageResponse response = BaggageResponse.from(baggage, paymentRequired);

        return ResponseEntity.ok(response);
    }
}


