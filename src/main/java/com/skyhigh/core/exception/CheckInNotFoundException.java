package com.skyhigh.core.exception;

import java.util.UUID;

public class CheckInNotFoundException extends RuntimeException {
    public CheckInNotFoundException(UUID checkInId) {
        super("Check-in not found with ID: " + checkInId);
    }
}
