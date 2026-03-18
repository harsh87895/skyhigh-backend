package com.skyhigh.core.dto;

import com.skyhigh.core.service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for payment processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private boolean success;
    private String transactionId;
    private BigDecimal amount;
    private Instant processedAt;
    private String message;

    public static PaymentResponse from(PaymentService.PaymentResult result) {
        return PaymentResponse.builder()
                .success(result.isSuccess())
                .transactionId(result.getTransactionId())
                .amount(result.getAmount())
                .processedAt(result.getProcessedAt())
                .message(result.getMessage())
                .build();
    }
}


