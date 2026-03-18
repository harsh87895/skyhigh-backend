package com.skyhigh.core.dto;

import com.skyhigh.core.model.entity.Baggage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for baggage validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggageResponse {

    private UUID baggageId;
    private BigDecimal weight;
    private Integer pieces;
    private Boolean paymentRequired;
    private BigDecimal excessWeight;
    private BigDecimal excessFee;
    private String message;

    public static BaggageResponse from(Baggage baggage, boolean paymentRequired) {
        String message = paymentRequired
            ? "Excess baggage detected. Payment required to proceed."
            : "Baggage within free allowance.";

        return BaggageResponse.builder()
            .baggageId(baggage.getBaggageId())
            .weight(baggage.getWeight())
            .pieces(baggage.getPieces())
            .paymentRequired(paymentRequired)
            .excessWeight(baggage.getExcessWeight())
            .excessFee(baggage.getExcessFee())
            .message(message)
            .build();
    }
}

