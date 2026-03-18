package com.skyhigh.core.exception;

import com.skyhigh.core.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers
 * Maps exceptions to appropriate HTTP status codes and error responses
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle seat unavailable (409 CONFLICT)
     */
    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleSeatUnavailable(
            SeatUnavailableException ex, WebRequest request) {
        log.warn("Seat unavailable: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("SEAT_UNAVAILABLE")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(error);
    }

    /**
     * Handle hold expired (410 GONE)
     */
    @ExceptionHandler(HoldExpiredException.class)
    public ResponseEntity<ErrorResponse> handleHoldExpired(
            HoldExpiredException ex, WebRequest request) {
        log.warn("Hold expired: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("HOLD_EXPIRED")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.GONE)
            .body(error);
    }

    /**
     * Handle optimistic lock exception (409 CONFLICT)
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockException ex, WebRequest request) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("CONCURRENT_UPDATE")
            .message("Resource was modified by another user. Please try again.")
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(error);
    }

    /**
     * Handle not found exceptions (404 NOT FOUND)
     */
    @ExceptionHandler({
        SeatNotFoundException.class,
        CheckInNotFoundException.class,
        HoldNotFoundException.class,
        BaggageNotFoundException.class,
        FlightNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
    }

    /**
     * Handle invalid state exceptions (400 BAD REQUEST)
     */
    @ExceptionHandler({
        InvalidCheckInStateException.class,
        InvalidHoldStateException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidState(
            RuntimeException ex, WebRequest request) {
        log.warn("Invalid state: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("INVALID_STATE")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * Handle check-in already exists (409 CONFLICT)
     */
    @ExceptionHandler(CheckInAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCheckInAlreadyExists(
            CheckInAlreadyExistsException ex, WebRequest request) {
        log.warn("Check-in already exists: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("ALREADY_CHECKED_IN")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(error);
    }

    /**
     * Handle payment exceptions (402 PAYMENT REQUIRED)
     */
    @ExceptionHandler({
        PaymentProcessingException.class,
        PaymentServiceUnavailableException.class
    })
    public ResponseEntity<ErrorResponse> handlePaymentError(
            RuntimeException ex, WebRequest request) {
        log.error("Payment error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("PAYMENT_ERROR")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.PAYMENT_REQUIRED)
            .body(error);
    }

    /**
     * Handle validation errors (400 BAD REQUEST)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", message);

        ErrorResponse error = ErrorResponse.builder()
            .error("VALIDATION_ERROR")
            .message(message)
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * Handle illegal argument (400 BAD REQUEST)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("BAD_REQUEST")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * Handle illegal state (400 BAD REQUEST)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("INVALID_OPERATION")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * Handle all other exceptions (500 INTERNAL SERVER ERROR)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);

        ErrorResponse error = ErrorResponse.builder()
            .error("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred. Please try again later.")
            .timestamp(Instant.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}

