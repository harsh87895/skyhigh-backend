package com.skyhigh.core.dto;

import com.skyhigh.core.model.entity.CheckIn;
import com.skyhigh.core.model.enums.CheckInStatus;
import com.skyhigh.core.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for check-in information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponse {

    private UUID checkInId;
    private String passengerId;
    private String flightId;
    private String bookingReference;
    private String seatNumber;
    private CheckInStatus status;
    private BigDecimal baggageWeight;
    private Boolean paymentRequired;
    private PaymentStatus paymentStatus;
    private Instant createdAt;
    private Instant completedAt;

    public static CheckInResponse from(CheckIn checkIn) {
        return CheckInResponse.builder()
            .checkInId(checkIn.getCheckInId())
            .passengerId(checkIn.getPassengerId())
            .flightId(checkIn.getFlightId())
            .bookingReference(checkIn.getBookingReference())
            .seatNumber(checkIn.getSeatNumber())
            .status(checkIn.getStatus())
            .baggageWeight(checkIn.getBaggageWeight())
            .paymentRequired(checkIn.getPaymentRequired())
            .paymentStatus(checkIn.getPaymentStatus())
            .createdAt(checkIn.getCreatedAt())
            .completedAt(checkIn.getCompletedAt())
            .build();
    }
}

