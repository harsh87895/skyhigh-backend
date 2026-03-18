package com.skyhigh.core.exception;

public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String seatNumber) {
        super("Seat is not available: " + seatNumber);
    }
}
