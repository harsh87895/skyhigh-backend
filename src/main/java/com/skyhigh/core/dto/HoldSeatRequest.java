package com.skyhigh.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for holding a seat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatRequest {

    @NotNull(message = "Check-in ID is required")
    private UUID checkInId;

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    @NotBlank(message = "Seat number is required")
    private String seatNumber;
}

