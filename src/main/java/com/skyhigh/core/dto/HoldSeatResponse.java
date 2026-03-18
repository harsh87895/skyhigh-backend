package com.skyhigh.core.dto;

import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.model.enums.HoldStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for seat hold information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatResponse {

    private UUID holdId;
    private UUID seatId;
    private String seatNumber;
    private HoldStatus status;
    private Instant heldAt;
    private Instant expiresAt;
    private Long remainingSeconds;

    public static HoldSeatResponse from(SeatHold hold, String seatNumber) {
        return HoldSeatResponse.builder()
            .holdId(hold.getHoldId())
            .seatId(hold.getSeatId())
            .seatNumber(seatNumber)
            .status(hold.getStatus())
            .heldAt(hold.getHeldAt())
            .expiresAt(hold.getExpiresAt())
            .remainingSeconds(hold.getRemainingSeconds())
            .build();
    }
}

