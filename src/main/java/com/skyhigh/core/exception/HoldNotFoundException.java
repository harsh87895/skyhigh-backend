package com.skyhigh.core.exception;

import java.util.UUID;

public class HoldNotFoundException extends RuntimeException {
    public HoldNotFoundException(UUID holdId) {
        super("Hold not found with ID: " + holdId);
    }
}
