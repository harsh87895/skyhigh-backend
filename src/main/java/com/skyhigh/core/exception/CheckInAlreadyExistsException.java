package com.skyhigh.core.exception;

public class CheckInAlreadyExistsException extends RuntimeException {
    public CheckInAlreadyExistsException(String passengerId, String flightId) {
        super("Check-in already exists for passenger " + passengerId + " on flight " + flightId);
    }
}
