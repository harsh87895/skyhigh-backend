package com.skyhigh.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting a check-in
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {

    @NotBlank(message = "Passenger ID is required")
    private String passengerId;

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;
}


