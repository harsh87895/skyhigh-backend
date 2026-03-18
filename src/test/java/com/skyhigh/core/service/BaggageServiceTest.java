package com.skyhigh.core.service;

import com.skyhigh.core.exception.BaggageNotFoundException;
import com.skyhigh.core.exception.CheckInNotFoundException;
import com.skyhigh.core.model.entity.Baggage;
import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.enums.CheckInStatus;
import com.skyhigh.core.model.enums.PaymentStatus;
import com.skyhigh.core.repository.BaggageRepository;
import com.skyhigh.core.repository.CheckInRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BaggageService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaggageService Tests")
class BaggageServiceTest {

    @Mock
    private BaggageRepository baggageRepository;

    @Mock
    private CheckInRepository checkInRepository;

    @InjectMocks
    private BaggageService baggageService;

    private UUID checkInId;
    private CheckIn activeCheckIn;

    @BeforeEach
    void setUp() {
        checkInId = UUID.randomUUID();

        ReflectionTestUtils.setField(baggageService, "maxFreeWeight", new BigDecimal("25.0"));
        ReflectionTestUtils.setField(baggageService, "feePerKg", new BigDecimal("10.0"));

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
    // validateBaggage Tests
    // =====================================================================

    @Test
    @DisplayName("Should validate baggage within free allowance without excess fee")
    void validateBaggage_withinFreeAllowance_noExcessFee() {
        // Given
        BigDecimal weight = new BigDecimal("20.0");
        Integer pieces = 1;

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(checkInRepository.save(any(CheckIn.class))).thenReturn(activeCheckIn);
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Baggage result = baggageService.validateBaggage(checkInId, weight, pieces);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getWeight()).isEqualByComparingTo(weight);
        assertThat(result.getPieces()).isEqualTo(pieces);
        assertThat(result.getExcessFee()).isNull();
        assertThat(result.getExcessWeight()).isNull();

        // Check-in should NOT be set to AWAITING_PAYMENT
        verify(checkInRepository).save(argThat(ci ->
                ci.getStatus() == CheckInStatus.IN_PROGRESS
        ));
    }

    @Test
    @DisplayName("Should calculate excess fee when baggage exceeds free allowance")
    void validateBaggage_exceedsFreeAllowance_calculatesExcessFee() {
        // Given
        BigDecimal weight = new BigDecimal("30.0"); // 5kg over 25kg limit
        Integer pieces = 2;

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(checkInRepository.save(any(CheckIn.class))).thenReturn(activeCheckIn);
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Baggage result = baggageService.validateBaggage(checkInId, weight, pieces);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExcessWeight()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(result.getExcessFee()).isEqualByComparingTo(new BigDecimal("50.0")); // 5 * 10

        // Check-in should be set to AWAITING_PAYMENT
        verify(checkInRepository).save(argThat(ci ->
                ci.getStatus() == CheckInStatus.AWAITING_PAYMENT &&
                Boolean.TRUE.equals(ci.getPaymentRequired()) &&
                ci.getPaymentStatus() == PaymentStatus.PENDING
        ));
    }

    @Test
    @DisplayName("Should throw CheckInNotFoundException when check-in does not exist")
    void validateBaggage_checkInNotFound_throwsException() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> baggageService.validateBaggage(checkInId, new BigDecimal("20.0"), 1))
                .isInstanceOf(CheckInNotFoundException.class);
    }

    @Test
    @DisplayName("Should set check-in baggageWeight for within-allowance baggage")
    void validateBaggage_withinAllowance_setsBaggageWeight() {
        // Given
        BigDecimal weight = new BigDecimal("15.0");
        ArgumentCaptor<CheckIn> checkInCaptor = ArgumentCaptor.forClass(CheckIn.class);

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(checkInRepository.save(checkInCaptor.capture())).thenReturn(activeCheckIn);
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        baggageService.validateBaggage(checkInId, weight, 1);

        // Then
        CheckIn savedCheckIn = checkInCaptor.getValue();
        assertThat(savedCheckIn.getBaggageWeight()).isEqualByComparingTo(weight);
    }

    @Test
    @DisplayName("Should save baggage with correct checkInId")
    void validateBaggage_savesBaggageWithCheckInId() {
        // Given
        BigDecimal weight = new BigDecimal("20.0");
        ArgumentCaptor<Baggage> baggageCaptor = ArgumentCaptor.forClass(Baggage.class);

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(activeCheckIn));
        when(checkInRepository.save(any(CheckIn.class))).thenReturn(activeCheckIn);
        when(baggageRepository.save(baggageCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        baggageService.validateBaggage(checkInId, weight, 1);

        // Then
        Baggage savedBaggage = baggageCaptor.getValue();
        assertThat(savedBaggage.getCheckInId()).isEqualTo(checkInId);
    }

    // =====================================================================
    // getBaggage Tests
    // =====================================================================

    @Test
    @DisplayName("Should return baggage when found for check-in")
    void getBaggage_baggageExists_returnsBaggage() {
        // Given
        Baggage baggage = Baggage.builder()
                .checkInId(checkInId)
                .weight(new BigDecimal("20.0"))
                .pieces(1)
                .build();

        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.of(baggage));

        // When
        Baggage result = baggageService.getBaggage(checkInId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCheckInId()).isEqualTo(checkInId);
        assertThat(result.getWeight()).isEqualByComparingTo(new BigDecimal("20.0"));
    }

    @Test
    @DisplayName("Should throw BaggageNotFoundException when baggage not found")
    void getBaggage_baggageNotFound_throwsException() {
        // Given
        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> baggageService.getBaggage(checkInId))
                .isInstanceOf(BaggageNotFoundException.class);
    }

    // =====================================================================
    // updateBaggagePayment Tests
    // =====================================================================

    @Test
    @DisplayName("Should update baggage with payment transaction ID")
    void updateBaggagePayment_success_updatesTransactionId() {
        // Given
        String transactionId = "TXN-12345678";
        Baggage baggage = Baggage.builder()
                .checkInId(checkInId)
                .weight(new BigDecimal("30.0"))
                .pieces(1)
                .excessWeight(new BigDecimal("5.0"))
                .excessFee(new BigDecimal("50.0"))
                .build();

        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.of(baggage));
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        baggageService.updateBaggagePayment(checkInId, transactionId);

        // Then
        verify(baggageRepository).save(argThat(b ->
                transactionId.equals(b.getPaymentTransactionId())
        ));
    }

    @Test
    @DisplayName("Should throw BaggageNotFoundException when updating payment for non-existing baggage")
    void updateBaggagePayment_baggageNotFound_throwsException() {
        // Given
        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> baggageService.updateBaggagePayment(checkInId, "TXN-123"))
                .isInstanceOf(BaggageNotFoundException.class);
    }
}

