package com.skyhigh.core.service;

import com.skyhigh.core.exception.BaggageNotFoundException;
import com.skyhigh.core.exception.CheckInNotFoundException;
import com.skyhigh.core.exception.InvalidCheckInStateException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private CheckInRepository checkInRepository;

    @Mock
    private BaggageRepository baggageRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID checkInId;
    private CheckIn awaitingPaymentCheckIn;
    private Baggage baggageWithExcessFee;

    @BeforeEach
    void setUp() {
        checkInId = UUID.randomUUID();

        awaitingPaymentCheckIn = CheckIn.builder()
                .checkInId(checkInId)
                .passengerId("P001")
                .flightId("SK123")
                .bookingReference("BK001")
                .status(CheckInStatus.AWAITING_PAYMENT)
                .paymentRequired(true)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        baggageWithExcessFee = Baggage.builder()
                .checkInId(checkInId)
                .weight(new BigDecimal("30.0"))
                .pieces(1)
                .excessWeight(new BigDecimal("5.0"))
                .excessFee(new BigDecimal("50.0"))
                .build();
    }

    // =====================================================================
    // processPayment Tests
    // =====================================================================

    @Test
    @DisplayName("Should successfully process payment and return success result")
    void processPayment_validRequest_returnsSuccess() {
        // Given
        BigDecimal amount = new BigDecimal("50.0");

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(awaitingPaymentCheckIn));
        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.of(baggageWithExcessFee));
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentService.PaymentResult result = paymentService.processPayment(checkInId, amount, "CREDIT_CARD");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).isNotNull().startsWith("TXN-");
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getMessage()).contains("successfully");
        assertThat(result.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update check-in status to IN_PROGRESS after successful payment")
    void processPayment_success_updatesCheckInStatus() {
        // Given
        BigDecimal amount = new BigDecimal("50.0");

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(awaitingPaymentCheckIn));
        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.of(baggageWithExcessFee));
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        paymentService.processPayment(checkInId, amount, "CREDIT_CARD");

        // Then
        verify(checkInRepository).save(argThat(ci ->
                ci.getStatus() == CheckInStatus.IN_PROGRESS &&
                ci.getPaymentStatus() == PaymentStatus.SUCCESS
        ));
    }

    @Test
    @DisplayName("Should set transactionId on baggage after successful payment")
    void processPayment_success_updatesBaggageTransactionId() {
        // Given
        BigDecimal amount = new BigDecimal("50.0");

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(awaitingPaymentCheckIn));
        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.of(baggageWithExcessFee));
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        paymentService.processPayment(checkInId, amount, "DEBIT_CARD");

        // Then
        verify(baggageRepository).save(argThat(b ->
                b.getPaymentTransactionId() != null &&
                b.getPaymentTransactionId().startsWith("TXN-")
        ));
    }

    @Test
    @DisplayName("Should throw CheckInNotFoundException when check-in does not exist")
    void processPayment_checkInNotFound_throwsException() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() ->
                paymentService.processPayment(checkInId, new BigDecimal("50.0"), "CREDIT_CARD"))
                .isInstanceOf(CheckInNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw InvalidCheckInStateException when check-in is not AWAITING_PAYMENT")
    void processPayment_checkInNotAwaitingPayment_throwsException() {
        // Given
        awaitingPaymentCheckIn.setStatus(CheckInStatus.IN_PROGRESS);
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(awaitingPaymentCheckIn));

        // When / Then
        assertThatThrownBy(() ->
                paymentService.processPayment(checkInId, new BigDecimal("50.0"), "CREDIT_CARD"))
                .isInstanceOf(InvalidCheckInStateException.class)
                .hasMessageContaining("not awaiting payment");
    }

    @Test
    @DisplayName("Should throw BaggageNotFoundException when baggage does not exist")
    void processPayment_baggageNotFound_throwsException() {
        // Given
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(awaitingPaymentCheckIn));
        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() ->
                paymentService.processPayment(checkInId, new BigDecimal("50.0"), "CREDIT_CARD"))
                .isInstanceOf(BaggageNotFoundException.class);
    }

    @Test
    @DisplayName("Should process payment even with amount mismatch (warning only)")
    void processPayment_amountMismatch_stillProcesses() {
        // Given
        BigDecimal differentAmount = new BigDecimal("60.0"); // mismatch with 50.0

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(awaitingPaymentCheckIn));
        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.of(baggageWithExcessFee));
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentService.PaymentResult result = paymentService.processPayment(checkInId, differentAmount, "CREDIT_CARD");

        // Then - should still succeed
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should process payment when baggage has no excess fee (null)")
    void processPayment_nullExcessFee_logsWarningAndProcesses() {
        // Given
        BigDecimal amount = new BigDecimal("50.0");
        baggageWithExcessFee.setExcessFee(null);

        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(awaitingPaymentCheckIn));
        when(baggageRepository.findByCheckInId(checkInId)).thenReturn(Optional.of(baggageWithExcessFee));
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentService.PaymentResult result = paymentService.processPayment(checkInId, amount, "CREDIT_CARD");

        // Then
        assertThat(result.isSuccess()).isTrue();
    }
}

