package com.skyhigh.core.service;

import com.skyhigh.core.exception.*;
import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.entity.Flight;
import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.model.enums.CheckInStatus;
import com.skyhigh.core.model.enums.FlightStatus;
import com.skyhigh.core.model.enums.HoldStatus;
import com.skyhigh.core.repository.CheckInRepository;
import com.skyhigh.core.repository.FlightRepository;
import com.skyhigh.core.repository.PassengerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CheckInService
 * Tests check-in workflow management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckInService Tests")
class CheckInServiceTest {

    @Mock
    private CheckInRepository checkInRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private SeatHoldService seatHoldService;

    @InjectMocks
    private CheckInService checkInService;

    private Flight activeFlight;
    private CheckIn activeCheckIn;
    private UUID checkInId;

    @BeforeEach
    void setUp() {
        checkInId = UUID.randomUUID();

        activeFlight = Flight.builder()
                .flightId("SK123")
                .flightNumber("SK123")
                .origin("BOM")
                .destination("DEL")
                .departureTime(Instant.now().plusSeconds(3600))
                .arrivalTime(Instant.now().plusSeconds(7200))
                .aircraftType("Boeing 737")
                .status(FlightStatus.SCHEDULED)
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
    // startCheckIn Tests
    // =====================================================================

    @Test
    @DisplayName("Should create a new check-in successfully when flight exists and no duplicate")
    void startCheckIn_validRequest_createsCheckIn() {
        // Given
        String passengerId = "P001";
        String flightId = "SK123";
        String bookingRef = "BK001";

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(activeFlight));
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.empty());
        when(checkInRepository.existsByPassengerIdAndFlightId(passengerId, flightId)).thenReturn(false);
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> {
            CheckIn ci = inv.getArgument(0);
            ci.setCheckInId(UUID.randomUUID());
            return ci;
        });

        // When
        CheckIn result = checkInService.startCheckIn(passengerId, flightId, bookingRef);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPassengerId()).isEqualTo(passengerId);
        assertThat(result.getFlightId()).isEqualTo(flightId);
        assertThat(result.getBookingReference()).isEqualTo(bookingRef);
        assertThat(result.getStatus()).isEqualTo(CheckInStatus.IN_PROGRESS);
        assertThat(result.getPaymentRequired()).isFalse();
    }

    @Test
    @DisplayName("Should throw FlightNotFoundException when flight does not exist")
    void startCheckIn_flightNotFound_throwsException() {
        // Given
        when(flightRepository.findById("INVALID_FLIGHT")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> checkInService.startCheckIn("P001", "INVALID_FLIGHT", "BK001"))
                .isInstanceOf(FlightNotFoundException.class)
                .hasMessageContaining("INVALID_FLIGHT");
    }

    @Test
    @DisplayName("Should throw CheckInAlreadyExistsException when passenger already has check-in for flight")
    void startCheckIn_duplicateCheckIn_throwsException() {
        // Given
        String passengerId = "P001";
        String flightId = "SK123";

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(activeFlight));
        when(checkInRepository.existsByPassengerIdAndFlightId(passengerId, flightId)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> checkInService.startCheckIn(passengerId, flightId, "BK001"))
                .isInstanceOf(CheckInAlreadyExistsException.class)
                .hasMessageContaining(passengerId)
                .hasMessageContaining(flightId);
    }

    @Test
    @DisplayName("Should save check-in with IN_PROGRESS status on creation")
    void startCheckIn_validRequest_savesWithInProgressStatus() {
        // Given
        ArgumentCaptor<CheckIn> captor = ArgumentCaptor.forClass(CheckIn.class);

        when(flightRepository.findById("SK123")).thenReturn(Optional.of(activeFlight));
        when(passengerRepository.findById("P001")).thenReturn(Optional.empty());
        when(checkInRepository.existsByPassengerIdAndFlightId("P001", "SK123")).thenReturn(false);
        when(checkInRepository.save(captor.capture())).thenAnswer(inv -> captor.getValue());

        // When
        checkInService.startCheckIn("P001", "SK123", "BK001");

        // Then
        CheckIn savedCheckIn = captor.getValue();
        assertThat(savedCheckIn.getStatus()).isEqualTo(CheckInStatus.IN_PROGRESS);
        assertThat(savedCheckIn.getPaymentRequired()).isFalse();
    }

    // =====================================================================
    // getCheckIn Tests
    // =====================================================================

    @Test
    @DisplayName("Should return check-in when it exists")
    void getCheckIn_exists_returnsCheckIn() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));

        // When
        CheckIn result = checkInService.getCheckIn(checkInId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCheckInId()).isEqualTo(checkInId);
    }

    @Test
    @DisplayName("Should throw CheckInNotFoundException when check-in does not exist")
    void getCheckIn_notFound_throwsException() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> checkInService.getCheckIn(checkInId))
                .isInstanceOf(CheckInNotFoundException.class)
                .hasMessageContaining(checkInId.toString());
    }

    // =====================================================================
    // completeCheckIn Tests
    // =====================================================================

    @Test
    @DisplayName("Should complete check-in successfully when seat is assigned and no active holds")
    void completeCheckIn_validState_completesCheckIn() {
        // Given
        activeCheckIn.setSeatNumber("12A");
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatHoldService.getActiveHolds(checkInId)).thenReturn(Collections.emptyList());
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        CheckIn result = checkInService.completeCheckIn(checkInId);

        // Then
        assertThat(result.getStatus()).isEqualTo(CheckInStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return already completed check-in without error")
    void completeCheckIn_alreadyCompleted_returnsExisting() {
        // Given
        activeCheckIn.setStatus(CheckInStatus.COMPLETED);
        activeCheckIn.setSeatNumber("12A");
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));

        // When
        CheckIn result = checkInService.completeCheckIn(checkInId);

        // Then
        assertThat(result.getStatus()).isEqualTo(CheckInStatus.COMPLETED);
        verify(checkInRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when check-in is awaiting payment")
    void completeCheckIn_awaitingPayment_throwsException() {
        // Given
        activeCheckIn.setStatus(CheckInStatus.AWAITING_PAYMENT);
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));

        // When / Then
        assertThatThrownBy(() -> checkInService.completeCheckIn(checkInId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("awaiting payment");
    }

    @Test
    @DisplayName("Should throw exception when no seat is assigned to check-in")
    void completeCheckIn_noSeatAssigned_throwsException() {
        // Given - no seat number set
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));

        // When / Then
        assertThatThrownBy(() -> checkInService.completeCheckIn(checkInId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No seat assigned");
    }

    @Test
    @DisplayName("Should throw exception when active seat hold still exists")
    void completeCheckIn_activeHoldExists_throwsException() {
        // Given
        activeCheckIn.setSeatNumber("12A");
        SeatHold activeHold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .checkInId(checkInId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(120))
                .build();

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatHoldService.getActiveHolds(checkInId)).thenReturn(List.of(activeHold));

        // When / Then
        assertThatThrownBy(() -> checkInService.completeCheckIn(checkInId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat hold is still active");
    }

    // =====================================================================
    // cancelCheckIn Tests
    // =====================================================================

    @Test
    @DisplayName("Should cancel check-in and release active holds")
    void cancelCheckIn_withActiveHold_releasesHoldAndCancels() {
        // Given
        SeatHold activeHold = SeatHold.builder()
                .holdId(UUID.randomUUID())
                .checkInId(checkInId)
                .status(HoldStatus.ACTIVE)
                .heldAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatHoldService.getActiveHolds(checkInId)).thenReturn(List.of(activeHold));

        // When
        checkInService.cancelCheckIn(checkInId);

        // Then
        verify(seatHoldService).releaseHold(activeHold);
        verify(checkInRepository).save(argThat(ci -> ci.getStatus() == CheckInStatus.EXPIRED));
    }

    @Test
    @DisplayName("Should cancel check-in without holds")
    void cancelCheckIn_noActiveHolds_setsExpiredStatus() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(seatHoldService.getActiveHolds(checkInId)).thenReturn(Collections.emptyList());

        // When
        checkInService.cancelCheckIn(checkInId);

        // Then
        verify(checkInRepository).save(argThat(ci -> ci.getStatus() == CheckInStatus.EXPIRED));
    }
}

