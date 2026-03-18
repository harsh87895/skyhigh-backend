package com.skyhigh.core.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing passenger baggage
 */
@Entity
@Table(name = "baggage", indexes = {
    @Index(name = "idx_baggage_checkin", columnList = "check_in_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Baggage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "baggage_id", updatable = false, nullable = false)
    private UUID baggageId;

    @Column(name = "check_in_id", nullable = false)
    private UUID checkInId;

    @Column(name = "weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "pieces", nullable = false)
    private Integer pieces;

    @Column(name = "excess_weight", precision = 10, scale = 2)
    private BigDecimal excessWeight;

    @Column(name = "excess_fee", precision = 10, scale = 2)
    private BigDecimal excessFee;

    @Column(name = "payment_transaction_id", length = 100)
    private String paymentTransactionId;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    @PrePersist
    protected void onCreate() {
        if (validatedAt == null) {
            validatedAt = Instant.now();
        }
    }
}
