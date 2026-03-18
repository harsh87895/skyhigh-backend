package com.skyhigh.core.model.entity;

import com.skyhigh.core.model.enums.HoldStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a temporary seat hold (120 seconds)
 */
@Entity
@Table(name = "seat_holds", indexes = {
    @Index(name = "idx_hold_seat", columnList = "seat_id"),
    @Index(name = "idx_hold_checkin", columnList = "check_in_id"),
    @Index(name = "idx_hold_status_expires", columnList = "status, expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "hold_id", updatable = false, nullable = false)
    private UUID holdId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(name = "check_in_id", nullable = false)
    private UUID checkInId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HoldStatus status;

    @Column(name = "held_at", nullable = false)
    private Instant heldAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * Calculate remaining seconds until expiration
     */
    public Long getRemainingSeconds() {
        if (status != HoldStatus.ACTIVE) {
            return 0L;
        }

        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return Math.max(0, remaining.getSeconds());
    }

    /**
     * Check if the hold has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
