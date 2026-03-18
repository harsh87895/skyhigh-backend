package com.skyhigh.core.exception;

public class FlightNotFoundException extends RuntimeException {
    public FlightNotFoundException(String flightId) {
        super("Flight not found: " + flightId);
    }
}
