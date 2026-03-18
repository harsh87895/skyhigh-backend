package com.skyhigh.core;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for SkyHigh Core Digital Check-In System
 *
 * This system handles:
 * - Conflict-free seat reservations with pessimistic locking
 * - Time-bound seat holds (120 seconds)
 * - Automated hold expiration via scheduled jobs
 * - Baggage validation and payment processing
 * - High-performance seat map queries with Redis caching
 */
@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "SkyHigh Core API",
        version = "1.0.0",
        description = "Digital Check-In System for SkyHigh Airlines - Manages seat reservations, " +
                     "baggage handling, and payment processing with high concurrency support",
        contact = @Contact(
            name = "SkyHigh Airlines Engineering",
            email = "engineering@skyhigh.com"
        ),
        license = @License(
            name = "Proprietary",
            url = "https://skyhigh.com/license"
        )
    )
)
public class SkyHighCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkyHighCoreApplication.class, args);
    }
}

