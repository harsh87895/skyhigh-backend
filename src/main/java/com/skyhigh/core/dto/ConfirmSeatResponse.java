package com.skyhigh.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for seat confirmation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmSeatResponse {

    private UUID holdId;
    private String seatNumber;
    private String status;
    private Instant confirmedAt;
    private String message;
}

