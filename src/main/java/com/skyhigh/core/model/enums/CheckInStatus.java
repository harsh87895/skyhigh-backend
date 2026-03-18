package com.skyhigh.core.model.enums;

/**
 * Check-in workflow status
 */
public enum CheckInStatus {
    IN_PROGRESS,       // Check-in started, in progress
    AWAITING_PAYMENT,  // Excess baggage payment required
    PAYMENT_FAILED,    // Payment processing failed
    COMPLETED,         // Check-in completed successfully
    EXPIRED            // Check-in expired without completion
}


