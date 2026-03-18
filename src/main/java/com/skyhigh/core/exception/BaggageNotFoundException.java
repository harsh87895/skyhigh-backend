package com.skyhigh.core.exception;

import java.util.UUID;

public class BaggageNotFoundException extends RuntimeException {
    public BaggageNotFoundException(UUID baggageId) {
        super("Baggage not found with ID: " + baggageId);
    }
}
