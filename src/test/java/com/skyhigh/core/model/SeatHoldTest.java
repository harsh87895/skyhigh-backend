package com.skyhigh.core.model;

import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.model.enums.HoldStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SeatHold entity domain logic
 * Tests expiry detection and remaining time calculation
 */
@DisplayName("SeatHold Entity Tests")
class SeatHoldTest {

    // =====================================================================
    // isExpired() Tests
    // =====================================================================

    @Test
    @DisplayName("Should return true when expiresAt is in the past")
    void isExpired_pastExpiry_returnsTrue() {
        // Given
        SeatHold hold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now().minusSeconds(200))
                .expiresAt(Instant.now().minusSeconds(80)) // expired 80 seconds ago
                .build();

        // When / Then
        assertThat(hold.isExpired()).isTrue();
    }

    @Test
    @DisplayName("Should return false when expiresAt is in the future")
    void isExpired_futureExpiry_returnsFalse() {
        // Given
        SeatHold hold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(90)) // 90 seconds remaining
                .build();

        // When / Then
        assertThat(hold.isExpired()).isFalse();
    }

    // =====================================================================
    // getRemainingSeconds() Tests
    // =====================================================================

    @Test
    @DisplayName("Should return approximate remaining seconds for an active hold")
    void getRemainingSeconds_activeHold_returnsPositiveValue() {
        // Given
        SeatHold hold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(90))
                .build();

        // When
        Long remaining = hold.getRemainingSeconds();

        // Then
        assertThat(remaining).isGreaterThan(85L); // Allow small time variance
        assertThat(remaining).isLessThanOrEqualTo(90L);
    }

    @Test
    @DisplayName("Should return 0 when hold is not ACTIVE (even if expiresAt is in the future)")
    void getRemainingSeconds_nonActiveHold_returnsZero() {
        // Given - CONFIRMED hold
        SeatHold confirmedHold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .status(HoldStatus.CONFIRMED) // not ACTIVE
                .heldAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(90))
                .build();

        // When
        Long remaining = confirmedHold.getRemainingSeconds();

        // Then
        assertThat(remaining).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should return 0 when hold has expired")
    void getRemainingSeconds_expiredHold_returnsZero() {
        // Given
        SeatHold expiredHold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now().minusSeconds(200))
                .expiresAt(Instant.now().minusSeconds(80)) // already expired
                .build();

        // When
        Long remaining = expiredHold.getRemainingSeconds();

        // Then
        assertThat(remaining).isEqualTo(0L);
    }

    // =====================================================================
    // Hold duration validation
    // =====================================================================

    @Test
    @DisplayName("A fresh hold should have approximately 120 seconds remaining")
    void freshHold_hasApproximate120SecondsRemaining() {
        // Given - simulating what SeatHoldService creates
        Instant now = Instant.now();
        SeatHold hold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .status(HoldStatus.ACTIVE)
                .heldAt(now)
                .expiresAt(now.plusSeconds(120))
                .build();

        // When
        Long remaining = hold.getRemainingSeconds();

        // Then
        assertThat(remaining).isGreaterThan(115L); // Allow for slight time delta
        assertThat(remaining).isLessThanOrEqualTo(120L);
        assertThat(hold.isExpired()).isFalse();
    }
}

