package com.skyhigh.core.service;

import com.skyhigh.core.model.entity.Seat;
import com.skyhigh.core.model.enums.SeatClass;
import com.skyhigh.core.model.enums.SeatPosition;
import com.skyhigh.core.model.enums.SeatStatus;
import com.skyhigh.core.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SeatService
 * Tests seat map retrieval, filtering, and availability checks
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeatService Tests")
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatService seatService;

    private List<Seat> allSeats;

    @BeforeEach
    void setUp() {
        allSeats = List.of(
            createSeat("1A", 1, "A", SeatClass.FIRST, SeatPosition.WINDOW, SeatStatus.AVAILABLE, "250.00"),
            createSeat("1B", 1, "B", SeatClass.FIRST, SeatPosition.AISLE, SeatStatus.CONFIRMED, "250.00"),
            createSeat("10A", 10, "A", SeatClass.BUSINESS, SeatPosition.WINDOW, SeatStatus.AVAILABLE, "180.00"),
            createSeat("10B", 10, "B", SeatClass.BUSINESS, SeatPosition.MIDDLE, SeatStatus.HELD, "180.00"),
            createSeat("20A", 20, "A", SeatClass.ECONOMY, SeatPosition.WINDOW, SeatStatus.AVAILABLE, "80.00"),
            createSeat("20B", 20, "B", SeatClass.ECONOMY, SeatPosition.MIDDLE, SeatStatus.AVAILABLE, "75.00"),
            createSeat("20C", 20, "C", SeatClass.ECONOMY, SeatPosition.AISLE, SeatStatus.CONFIRMED, "75.00")
        );
    }

    // =====================================================================
    // getSeatMap Tests
    // =====================================================================

    @Test
    @DisplayName("Should return all seats for a given flight")
    void getSeatMap_validFlight_returnsAllSeats() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        List<Seat> result = seatService.getSeatMap("SK123");

        // Then
        assertThat(result).hasSize(7);
        verify(seatRepository).findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123");
    }

    @Test
    @DisplayName("Should return empty list when no seats exist for flight")
    void getSeatMap_noSeats_returnsEmptyList() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("EMPTY_FLIGHT"))
                .thenReturn(List.of());

        // When
        List<Seat> result = seatService.getSeatMap("EMPTY_FLIGHT");

        // Then
        assertThat(result).isEmpty();
    }

    // =====================================================================
    // getAvailableSeats Tests
    // =====================================================================

    @Test
    @DisplayName("Should return only AVAILABLE seats, excluding HELD and CONFIRMED")
    void getAvailableSeats_filtersCorrectly() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        List<Seat> result = seatService.getAvailableSeats("SK123");

        // Then
        assertThat(result).hasSize(4); // 1A, 10A, 20A, 20B are AVAILABLE
        assertThat(result).allMatch(seat -> seat.getStatus() == SeatStatus.AVAILABLE);
    }

    // =====================================================================
    // getFilteredSeats Tests
    // =====================================================================

    @Test
    @DisplayName("Should filter seats by class ECONOMY only")
    void getFilteredSeats_byClass_returnsMatchingSeats() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        List<Seat> result = seatService.getFilteredSeats("SK123", SeatClass.ECONOMY, null);

        // Then
        assertThat(result).hasSize(3); // 20A, 20B, 20C
        assertThat(result).allMatch(seat -> seat.getSeatClass() == SeatClass.ECONOMY);
    }

    @Test
    @DisplayName("Should filter seats by WINDOW position only")
    void getFilteredSeats_byPosition_returnsMatchingSeats() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        List<Seat> result = seatService.getFilteredSeats("SK123", null, SeatPosition.WINDOW);

        // Then
        assertThat(result).hasSize(3); // 1A, 10A, 20A
        assertThat(result).allMatch(seat -> seat.getPosition() == SeatPosition.WINDOW);
    }

    @Test
    @DisplayName("Should filter seats by both class and position")
    void getFilteredSeats_byClassAndPosition_returnsMatchingSeats() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        List<Seat> result = seatService.getFilteredSeats("SK123", SeatClass.ECONOMY, SeatPosition.WINDOW);

        // Then
        assertThat(result).hasSize(1); // Only 20A
        assertThat(result.get(0).getSeatNumber()).isEqualTo("20A");
    }

    @Test
    @DisplayName("Should return all seats when no filter is applied")
    void getFilteredSeats_noFilter_returnsAll() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        List<Seat> result = seatService.getFilteredSeats("SK123", null, null);

        // Then
        assertThat(result).hasSize(7);
    }

    // =====================================================================
    // getAvailableSeatsCount Tests
    // =====================================================================

    @Test
    @DisplayName("Should return correct count of available seats")
    void getAvailableSeatsCount_returnsCount() {
        // Given
        when(seatRepository.countByFlightIdAndStatus("SK123", SeatStatus.AVAILABLE)).thenReturn(4L);

        // When
        long count = seatService.getAvailableSeatsCount("SK123");

        // Then
        assertThat(count).isEqualTo(4L);
    }

    // =====================================================================
    // isSeatAvailable Tests
    // =====================================================================

    @Test
    @DisplayName("Should return true when seat is AVAILABLE")
    void isSeatAvailable_availableSeat_returnsTrue() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        boolean result = seatService.isSeatAvailable("SK123", "20A");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when seat is HELD")
    void isSeatAvailable_heldSeat_returnsFalse() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        boolean result = seatService.isSeatAvailable("SK123", "10B");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when seat is CONFIRMED")
    void isSeatAvailable_confirmedSeat_returnsFalse() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        boolean result = seatService.isSeatAvailable("SK123", "1B");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when seat number does not exist")
    void isSeatAvailable_nonExistentSeat_returnsFalse() {
        // Given
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc("SK123"))
                .thenReturn(allSeats);

        // When
        boolean result = seatService.isSeatAvailable("SK123", "99Z");

        // Then
        assertThat(result).isFalse();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Seat createSeat(String seatNumber, int row, String col, SeatClass cls,
                            SeatPosition pos, SeatStatus status, String price) {
        return Seat.builder()
                .seatId(UUID.randomUUID())
                .flightId("SK123")
                .seatNumber(seatNumber)
                .rowNumber(row)
                .columnLetter(col)
                .seatClass(cls)
                .position(pos)
                .status(status)
                .price(new BigDecimal(price))
                .version(0L)
                .build();
    }
}

