package com.skyhigh.core.dto;

import com.skyhigh.core.model.entity.Seat;
import com.skyhigh.core.model.enums.SeatClass;
import com.skyhigh.core.model.enums.SeatPosition;
import com.skyhigh.core.model.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for seat map
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapResponse {

    private String flightId;
    private String aircraftType;
    private Integer totalSeats;
    private Long availableSeats;
    private List<SeatDTO> seats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatDTO {
        private UUID seatId;
        private String seatNumber;
        private Integer row;
        private String column;
        private SeatClass seatClass;
        private SeatPosition position;
        private BigDecimal price;
        private String status;  // AVAILABLE or UNAVAILABLE (hiding HELD/CONFIRMED)

        public static SeatDTO from(Seat seat) {
            // Hide specific status - only show AVAILABLE or UNAVAILABLE
            String publicStatus = seat.getStatus() == SeatStatus.AVAILABLE
                ? "AVAILABLE"
                : "UNAVAILABLE";

            return SeatDTO.builder()
                .seatId(seat.getSeatId())
                .seatNumber(seat.getSeatNumber())
                .row(seat.getRowNumber())
                .column(seat.getColumnLetter())
                .seatClass(seat.getSeatClass())
                .position(seat.getPosition())
                .price(seat.getPrice())
                .status(publicStatus)
                .build();
        }
    }

    public static SeatMapResponse from(String flightId, List<Seat> seats) {
        long availableCount = seats.stream()
            .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
            .count();

        List<SeatDTO> seatDTOs = seats.stream()
            .map(SeatDTO::from)
            .collect(Collectors.toList());

        return SeatMapResponse.builder()
            .flightId(flightId)
            .totalSeats(seats.size())
            .availableSeats(availableCount)
            .seats(seatDTOs)
            .build();
    }
}

