package com.skyhigh.core.exception;

import java.util.UUID;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(UUID holdId) {
        super("Hold has expired: " + holdId);
    }
}
