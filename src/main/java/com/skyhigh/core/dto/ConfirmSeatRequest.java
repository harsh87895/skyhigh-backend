package com.skyhigh.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for confirming a seat hold
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmSeatRequest {

    @NotNull(message = "Hold ID is required")
    private UUID holdId;

    @NotNull(message = "Check-in ID is required")
    private UUID checkInId;
}

