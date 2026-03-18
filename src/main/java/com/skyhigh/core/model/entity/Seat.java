package com.skyhigh.core.model.entity;

import com.skyhigh.core.model.enums.SeatClass;
import com.skyhigh.core.model.enums.SeatPosition;
import com.skyhigh.core.model.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing a seat on a flight
 */
@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_seat_flight_number", columnList = "flight_id, seat_number"),
    @Index(name = "idx_seat_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "seat_id", updatable = false, nullable = false)
    private UUID seatId;

    @Column(name = "flight_id", nullable = false, length = 20)
    private String flightId;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_letter", nullable = false, length = 2)
    private String columnLetter;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class", nullable = false, length = 20)
    private SeatClass seatClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 20)
    private SeatPosition position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatStatus status;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Version
    @Column(name = "version")
    private Long version;  // For optimistic locking
}
