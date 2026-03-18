package com.skyhigh.core.model.entity;

import com.skyhigh.core.model.enums.CheckInStatus;
import com.skyhigh.core.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a passenger check-in session
 */
@Entity
@Table(name = "check_ins", indexes = {
    @Index(name = "idx_checkin_passenger_flight", columnList = "passenger_id, flight_id", unique = true),
    @Index(name = "idx_checkin_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "check_in_id", updatable = false, nullable = false)
    private UUID checkInId;

    @Column(name = "passenger_id", nullable = false, length = 50)
    private String passengerId;

    @Column(name = "flight_id", nullable = false, length = 20)
    private String flightId;

    @Column(name = "booking_reference", nullable = false, length = 10)
    private String bookingReference;

    @Column(name = "seat_number", length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CheckInStatus status;

    @Column(name = "baggage_weight", precision = 10, scale = 2)
    private BigDecimal baggageWeight;

    @Column(name = "payment_required")
    private Boolean paymentRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
