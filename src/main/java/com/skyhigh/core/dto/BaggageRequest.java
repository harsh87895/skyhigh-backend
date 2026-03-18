package com.skyhigh.core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for baggage validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggageRequest {

    @NotNull(message = "Check-in ID is required")
    private UUID checkInId;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be greater than 0")
    private BigDecimal weight;

    @NotNull(message = "Number of pieces is required")
    @Min(value = 1, message = "At least one piece of baggage required")
    private Integer pieces;
}

