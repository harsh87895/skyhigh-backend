package com.skyhigh.core.exception;

import java.util.UUID;

public class SeatNotFoundException extends RuntimeException {
    public SeatNotFoundException(String seatNumber) {
        super("Seat not found: " + seatNumber);
    }

    public SeatNotFoundException(UUID seatId) {
        super("Seat not found with ID: " + seatId);
    }
}
