package com.skyhigh.core.model.enums;

/**
 * Seat hold status
 */
public enum HoldStatus {
    ACTIVE,      // Hold is currently active
    EXPIRED,     // Hold has expired (120 seconds passed)
    CONFIRMED,   // Hold was confirmed (seat assigned)
    RELEASED     // Hold was manually released
}


