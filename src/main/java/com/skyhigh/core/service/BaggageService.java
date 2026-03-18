package com.skyhigh.core.service;

import com.skyhigh.core.exception.BaggageNotFoundException;
import com.skyhigh.core.exception.CheckInNotFoundException;
import com.skyhigh.core.model.entity.Baggage;
import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.enums.CheckInStatus;
import com.skyhigh.core.model.enums.PaymentStatus;
import com.skyhigh.core.repository.BaggageRepository;
import com.skyhigh.core.repository.CheckInRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for baggage validation and fee calculation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BaggageService {

    private final BaggageRepository baggageRepository;
    private final CheckInRepository checkInRepository;

    @Value("${app.baggage.max-free-weight:25.0}")
    private BigDecimal maxFreeWeight;

    @Value("${app.baggage.fee-per-kg:10.0}")
    private BigDecimal feePerKg;

    /**
     * Validate baggage weight and calculate excess fee
     */
    @Transactional
    public Baggage validateBaggage(UUID checkInId, BigDecimal weight, Integer pieces) {
        log.info("Validating baggage for check-in {}: weight={}kg, pieces={}",
                 checkInId, weight, pieces);

        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new CheckInNotFoundException(checkInId));

        // Create baggage record
        Baggage baggage = Baggage.builder()
            .checkInId(checkInId)
            .weight(weight)
            .pieces(pieces)
            .build();

        // Check if payment required
        boolean paymentRequired = weight.compareTo(maxFreeWeight) > 0;

        if (paymentRequired) {
            BigDecimal excessWeight = weight.subtract(maxFreeWeight);
            BigDecimal excessFee = excessWeight.multiply(feePerKg);

            baggage.setExcessWeight(excessWeight);
            baggage.setExcessFee(excessFee);

            // Update check-in status to awaiting payment
            checkIn.setStatus(CheckInStatus.AWAITING_PAYMENT);
            checkIn.setPaymentRequired(true);
            checkIn.setPaymentStatus(PaymentStatus.PENDING);
            checkIn.setBaggageWeight(weight);

            log.info("Excess baggage detected: {}kg over limit. Fee: ${}",
                     excessWeight, excessFee);
        } else {
            checkIn.setBaggageWeight(weight);
            log.info("Baggage within free allowance: {}kg", weight);
        }

        checkInRepository.save(checkIn);
        baggage = baggageRepository.save(baggage);

        return baggage;
    }

    /**
     * Get baggage information for a check-in
     */
    @Transactional(readOnly = true)
    public Baggage getBaggage(UUID checkInId) {
        return baggageRepository.findByCheckInId(checkInId)
            .orElseThrow(() -> new BaggageNotFoundException(checkInId));
    }

    /**
     * Update baggage with payment transaction ID
     */
    @Transactional
    public void updateBaggagePayment(UUID checkInId, String transactionId) {
        Baggage baggage = baggageRepository.findByCheckInId(checkInId)
            .orElseThrow(() -> new BaggageNotFoundException(checkInId));

        baggage.setPaymentTransactionId(transactionId);
        baggageRepository.save(baggage);

        log.info("Updated baggage payment for check-in {} with transaction {}",
                 checkInId, transactionId);
    }
}

