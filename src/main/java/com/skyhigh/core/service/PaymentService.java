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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for processing excess baggage payments
 * Uses mock payment that always succeeds
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final CheckInRepository checkInRepository;
    private final BaggageRepository baggageRepository;

    /**
     * Process payment for excess baggage (MOCK - always succeeds)
     */
    @Transactional
    public PaymentResult processPayment(UUID checkInId, BigDecimal amount, String paymentMethod) {
        log.info("Processing payment for check-in {}: amount=${}, method={}",
                 checkInId, amount, paymentMethod);

        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new CheckInNotFoundException(checkInId));

        if (checkIn.getStatus() != CheckInStatus.AWAITING_PAYMENT) {
            throw new InvalidCheckInStateException(
                "Check-in is not awaiting payment. Current status: " + checkIn.getStatus());
        }

        Baggage baggage = baggageRepository.findByCheckInId(checkInId)
            .orElseThrow(() -> new BaggageNotFoundException(checkInId));

        // Validate amount matches excess fee
        if (baggage.getExcessFee() == null ||
            baggage.getExcessFee().compareTo(amount) != 0) {
            log.warn("Payment amount mismatch. Expected: {}, Received: {}",
                     baggage.getExcessFee(), amount);
        }

        try {
            // MOCK PAYMENT - Always succeeds
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            log.info("Mock payment processing... Transaction ID: {}", transactionId);

            // Simulate payment processing delay
            Thread.sleep(500);

            // Payment successful
            baggage.setPaymentTransactionId(transactionId);
            baggageRepository.save(baggage);

            checkIn.setPaymentStatus(PaymentStatus.SUCCESS);
            checkIn.setStatus(CheckInStatus.IN_PROGRESS);  // Resume check-in
            checkInRepository.save(checkIn);

            log.info("Payment successful for check-in {}: transaction={}, amount=${}",
                     checkInId, transactionId, amount);

            return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .amount(amount)
                .processedAt(Instant.now())
                .message("Payment processed successfully")
                .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            // Payment failed
            checkIn.setPaymentStatus(PaymentStatus.FAILED);
            checkIn.setStatus(CheckInStatus.PAYMENT_FAILED);
            checkInRepository.save(checkIn);

            log.error("Payment processing error for check-in {}", checkInId, e);

            return PaymentResult.builder()
                .success(false)
                .amount(amount)
                .processedAt(Instant.now())
                .message("Payment processing failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Payment result DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class PaymentResult {
        private boolean success;
        private String transactionId;
        private BigDecimal amount;
        private Instant processedAt;
        private String message;
    }
}

