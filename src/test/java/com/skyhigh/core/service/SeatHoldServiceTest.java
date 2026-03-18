package com.skyhigh.core.service;

import com.skyhigh.core.exception.*;
import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.entity.Seat;
import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.model.enums.CheckInStatus;
import com.skyhigh.core.model.enums.HoldStatus;
import com.skyhigh.core.model.enums.SeatClass;
import com.skyhigh.core.model.enums.SeatPosition;
import com.skyhigh.core.model.enums.SeatStatus;
import com.skyhigh.core.repository.CheckInRepository;
import com.skyhigh.core.repository.SeatHoldRepository;
import com.skyhigh.core.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SeatHoldService
 * Tests seat hold, confirm, release, and expiration logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeatHoldService Tests")
class SeatHoldServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatHoldRepository seatHoldRepository;

    @Mock
    private CheckInRepository checkInRepository;

    @InjectMocks
    private SeatHoldService seatHoldService;

    private UUID checkInId;
    private UUID seatId;
    private UUID holdId;
    private Seat availableSeat;
    private CheckIn activeCheckIn;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatHoldService, "holdDurationSeconds", 120);

        checkInId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        holdId = UUID.randomUUID();

        availableSeat = Seat.builder()
                .seatId(seatId)
                .flightId("SK123")
                .seatNumber("12A")
                .rowNumber(12)
                .columnLetter("A")
                .seatClass(SeatClass.ECONOMY)
                .position(SeatPosition.WINDOW)
                .status(SeatStatus.AVAILABLE)
                .price(new BigDecimal("150.00"))
                .version(0L)
                .build();

        activeCheckIn = CheckIn.builder()
                .checkInId(checkInId)
                .passengerId("P001")
                .flightId("SK123")
                .bookingReference("BK001")
                .status(CheckInStatus.IN_PROGRESS)
                .paymentRequired(false)
                .build();
    }

    // =====================================================================
    // holdSeat Tests
    // =====================================================================

    @Test
    @DisplayName("Should successfully hold an available seat")
    void holdSeat_availableSeat_createsHold() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatRepository.findByFlightIdAndSeatNumberWithLock("SK123", "12A"))
                .thenReturn(Optional.of(availableSeat));
        when(seatHoldRepository.findByCheckInIdAndStatus(checkInId, HoldStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(seatRepository.save(any(Seat.class))).thenReturn(availableSeat);
        when(seatHoldRepository.save(any(SeatHold.class))).thenAnswer(inv -> {
            SeatHold hold = inv.getArgument(0);
            hold.setHoldId(holdId);
            return hold;
        });

        // When
        SeatHold result = seatHoldService.holdSeat(checkInId, "SK123", "12A");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSeatId()).isEqualTo(seatId);
        assertThat(result.getCheckInId()).isEqualTo(checkInId);
        assertThat(result.getStatus()).isEqualTo(HoldStatus.ACTIVE);
        assertThat(result.getExpiresAt()).isAfter(Instant.now());

        // Verify seat status was updated to HELD
        verify(seatRepository).save(argThat(s -> s.getStatus() == SeatStatus.HELD));
    }

    @Test
    @DisplayName("Should set hold expiration to exactly 120 seconds from now")
    void holdSeat_availableSeat_setsCorrectExpiration() {
        // Given
        ArgumentCaptor<SeatHold> holdCaptor = ArgumentCaptor.forClass(SeatHold.class);

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatRepository.findByFlightIdAndSeatNumberWithLock("SK123", "12A"))
                .thenReturn(Optional.of(availableSeat));
        when(seatHoldRepository.findByCheckInIdAndStatus(checkInId, HoldStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(seatRepository.save(any(Seat.class))).thenReturn(availableSeat);
        when(seatHoldRepository.save(holdCaptor.capture())).thenAnswer(inv -> holdCaptor.getValue());

        // When
        Instant before = Instant.now();
        seatHoldService.holdSeat(checkInId, "SK123", "12A");
        Instant after = Instant.now();

        // Then
        SeatHold capturedHold = holdCaptor.getValue();
        Instant expectedExpiry = before.plusSeconds(120);
        Instant expectedExpiryMax = after.plusSeconds(120);

        assertThat(capturedHold.getExpiresAt()).isAfterOrEqualTo(expectedExpiry);
        assertThat(capturedHold.getExpiresAt()).isBeforeOrEqualTo(expectedExpiryMax);
    }

    @Test
    @DisplayName("Should throw SeatUnavailableException when seat is HELD by another passenger")
    void holdSeat_seatAlreadyHeld_throwsException() {
        // Given
        availableSeat.setStatus(SeatStatus.HELD);

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatRepository.findByFlightIdAndSeatNumberWithLock("SK123", "12A"))
                .thenReturn(Optional.of(availableSeat));

        // When / Then
        assertThatThrownBy(() -> seatHoldService.holdSeat(checkInId, "SK123", "12A"))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("12A");
    }

    @Test
    @DisplayName("Should throw SeatUnavailableException when seat is CONFIRMED")
    void holdSeat_seatConfirmed_throwsException() {
        // Given
        availableSeat.setStatus(SeatStatus.CONFIRMED);

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatRepository.findByFlightIdAndSeatNumberWithLock("SK123", "12A"))
                .thenReturn(Optional.of(availableSeat));

        // When / Then
        assertThatThrownBy(() -> seatHoldService.holdSeat(checkInId, "SK123", "12A"))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    @DisplayName("Should throw CheckInNotFoundException when check-in does not exist")
    void holdSeat_checkInNotFound_throwsException() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> seatHoldService.holdSeat(checkInId, "SK123", "12A"))
                .isInstanceOf(CheckInNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw InvalidCheckInStateException when check-in is not IN_PROGRESS")
    void holdSeat_checkInNotInProgress_throwsException() {
        // Given
        activeCheckIn.setStatus(CheckInStatus.AWAITING_PAYMENT);

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));

        // When / Then
        assertThatThrownBy(() -> seatHoldService.holdSeat(checkInId, "SK123", "12A"))
                .isInstanceOf(InvalidCheckInStateException.class)
                .hasMessageContaining("not in progress");
    }

    @Test
    @DisplayName("Should throw SeatNotFoundException when seat does not exist")
    void holdSeat_seatNotFound_throwsException() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatRepository.findByFlightIdAndSeatNumberWithLock("SK123", "99Z"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> seatHoldService.holdSeat(checkInId, "SK123", "99Z"))
                .isInstanceOf(SeatNotFoundException.class)
                .hasMessageContaining("99Z");
    }

    @Test
    @DisplayName("Should release previous hold when passenger selects a new seat")
    void holdSeat_withPreviousHold_releasesPreviousHold() {
        // Given
        UUID previousSeatId = UUID.randomUUID();
        Seat previousSeat = Seat.builder()
                .seatId(previousSeatId)
                .flightId("SK123")
                .seatNumber("11A")
                .status(SeatStatus.HELD)
                .build();

        SeatHold previousHold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .seatId(previousSeatId)
                .checkInId(checkInId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(90))
                .build();

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatRepository.findByFlightIdAndSeatNumberWithLock("SK123", "12A"))
                .thenReturn(Optional.of(availableSeat));
        when(seatHoldRepository.findByCheckInIdAndStatus(checkInId, HoldStatus.ACTIVE))
                .thenReturn(List.of(previousHold));
        when(seatRepository.findById(previousSeatId)).thenReturn(Optional.of(previousSeat));
        when(seatRepository.save(any(Seat.class))).thenReturn(availableSeat);
        when(seatHoldRepository.save(any(SeatHold.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        seatHoldService.holdSeat(checkInId, "SK123", "12A");

        // Then - previous hold should be released
        verify(seatHoldRepository, atLeastOnce()).save(argThat(
                h -> h.getHoldId() != null && h.getStatus() == HoldStatus.RELEASED
        ));
    }

    // =====================================================================
    // confirmHold Tests
    // =====================================================================

    @Test
    @DisplayName("Should confirm an active hold and set seat to CONFIRMED")
    void confirmHold_activeHold_confirmsSuccessfully() {
        // Given
        SeatHold activeHold = SeatHold.builder()
                .holdId(holdId)
                .seatId(seatId)
                .checkInId(checkInId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(90))
                .build();

        availableSeat.setStatus(SeatStatus.HELD);

        when(seatHoldRepository.findById(holdId)).thenReturn(Optional.of(activeHold));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(availableSeat));
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatHoldRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(seatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(checkInRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        seatHoldService.confirmHold(holdId, checkInId);

        // Then
        verify(seatRepository).save(argThat(s -> s.getStatus() == SeatStatus.CONFIRMED));
        verify(seatHoldRepository).save(argThat(h -> h.getStatus() == HoldStatus.CONFIRMED));
        verify(checkInRepository).save(argThat(ci -> "12A".equals(ci.getSeatNumber())));
    }

    @Test
    @DisplayName("Should throw HoldExpiredException when hold has expired")
    void confirmHold_expiredHold_throwsHoldExpiredException() {
        // Given
        SeatHold expiredHold = SeatHold.builder()
                .holdId(holdId)
                .seatId(seatId)
                .checkInId(checkInId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now().minusSeconds(200))
                .expiresAt(Instant.now().minusSeconds(80)) // already expired
                .build();

        when(seatHoldRepository.findById(holdId)).thenReturn(Optional.of(expiredHold));

        // When / Then
        assertThatThrownBy(() -> seatHoldService.confirmHold(holdId, checkInId))
                .isInstanceOf(HoldExpiredException.class);
    }

    @Test
    @DisplayName("Should throw InvalidHoldStateException when hold is not ACTIVE")
    void confirmHold_alreadyConfirmedHold_throwsException() {
        // Given
        SeatHold confirmedHold = SeatHold.builder()
                .holdId(holdId)
                .seatId(seatId)
                .checkInId(checkInId)
                .status(HoldStatus.CONFIRMED) // already confirmed
                .heldAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(90))
                .build();

        when(seatHoldRepository.findById(holdId)).thenReturn(Optional.of(confirmedHold));

        // When / Then
        assertThatThrownBy(() -> seatHoldService.confirmHold(holdId, checkInId))
                .isInstanceOf(InvalidHoldStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("Should throw InvalidHoldStateException when hold belongs to different check-in")
    void confirmHold_wrongCheckIn_throwsException() {
        // Given
        UUID differentCheckInId = UUID.randomUUID();
        SeatHold holdForDifferentCheckIn = SeatHold.builder()
                .holdId(holdId)
                .seatId(seatId)
                .checkInId(differentCheckInId) // different check-in
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(120))
                .build();

        when(seatHoldRepository.findById(holdId)).thenReturn(Optional.of(holdForDifferentCheckIn));

        // When / Then
        assertThatThrownBy(() -> seatHoldService.confirmHold(holdId, checkInId))
                .isInstanceOf(InvalidHoldStateException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    @DisplayName("Should throw HoldNotFoundException when hold does not exist")
    void confirmHold_holdNotFound_throwsException() {
        // Given
        when(seatHoldRepository.findById(holdId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> seatHoldService.confirmHold(holdId, checkInId))
                .isInstanceOf(HoldNotFoundException.class);
    }

    // =====================================================================
    // releaseHold Tests
    // =====================================================================

    @Test
    @DisplayName("Should release an active hold and set seat back to AVAILABLE")
    void releaseHold_activeHold_releasesAndRestoresSeat() {
        // Given
        availableSeat.setStatus(SeatStatus.HELD);

        SeatHold activeHold = SeatHold.builder()
                .holdId(holdId)
                .seatId(seatId)
                .checkInId(checkInId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(90))
                .build();

        when(seatRepository.findById(seatId)).thenReturn(Optional.of(availableSeat));
        when(seatHoldRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(seatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        seatHoldService.releaseHold(activeHold);

        // Then
        verify(seatRepository).save(argThat(s -> s.getStatus() == SeatStatus.AVAILABLE));
        verify(seatHoldRepository).save(argThat(h -> h.getStatus() == HoldStatus.RELEASED));
    }

    @Test
    @DisplayName("Should not release a hold that is already CONFIRMED")
    void releaseHold_confirmedHold_doesNothing() {
        // Given
        SeatHold confirmedHold = SeatHold.builder()
                .holdId(holdId)
                .seatId(seatId)
                .checkInId(checkInId)
                .status(HoldStatus.CONFIRMED) // not active
                .heldAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(90))
                .build();

        // When
        seatHoldService.releaseHold(confirmedHold);

        // Then
        verify(seatRepository, never()).save(any());
        verify(seatHoldRepository, never()).save(any());
    }

    // =====================================================================
    // findExpiredHolds Tests
    // =====================================================================

    @Test
    @DisplayName("Should return list of expired holds")
    void findExpiredHolds_returnsExpiredHolds() {
        // Given
        SeatHold expiredHold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .seatId(seatId)
                .checkInId(checkInId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now().minusSeconds(200))
                .expiresAt(Instant.now().minusSeconds(80))
                .build();

        when(seatHoldRepository.findExpiredHolds(eq(HoldStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(expiredHold));

        // When
        List<SeatHold> result = seatHoldService.findExpiredHolds();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(HoldStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return empty list when no holds are expired")
    void findExpiredHolds_noExpiredHolds_returnsEmptyList() {
        // Given
        when(seatHoldRepository.findExpiredHolds(eq(HoldStatus.ACTIVE), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<SeatHold> result = seatHoldService.findExpiredHolds();

        // Then
        assertThat(result).isEmpty();
    }

    // =====================================================================
    // isHoldValid Tests
    // =====================================================================

    @Test
    @DisplayName("Should return true for a valid, non-expired active hold")
    void isHoldValid_activeNotExpired_returnsTrue() {
        // Given
        SeatHold validHold = SeatHold.builder()
                .holdId(holdId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(90))
                .build();

        when(seatHoldRepository.findById(holdId)).thenReturn(Optional.of(validHold));

        // When
        boolean result = seatHoldService.isHoldValid(holdId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for an expired hold")
    void isHoldValid_expiredHold_returnsFalse() {
        // Given
        SeatHold expiredHold = SeatHold.builder()
                .holdId(holdId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now().minusSeconds(200))
                .expiresAt(Instant.now().minusSeconds(80)) // expired
                .build();

        when(seatHoldRepository.findById(holdId)).thenReturn(Optional.of(expiredHold));

        // When
        boolean result = seatHoldService.isHoldValid(holdId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when hold does not exist")
    void isHoldValid_holdNotFound_returnsFalse() {
        // Given
        when(seatHoldRepository.findById(holdId)).thenReturn(Optional.empty());

        // When
        boolean result = seatHoldService.isHoldValid(holdId);

        // Then
        assertThat(result).isFalse();
    }
}

