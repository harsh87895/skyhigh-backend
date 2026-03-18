package com.skyhigh.core.controller;

import com.skyhigh.core.dto.PaymentRequest;
import com.skyhigh.core.dto.PaymentResponse;
import com.skyhigh.core.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for payment processing (MOCK - always succeeds)
 */
@RestController
@RequestMapping("/api/v1/payment")
@Tag(name = "Payment", description = "Payment processing APIs (MOCK)")
@Slf4j
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process payment for excess baggage (MOCK - always succeeds)
     */
    @PostMapping("/process")
    @Operation(
        summary = "Process payment (MOCK)",
        description = "Process excess baggage payment. This is a MOCK endpoint that always succeeds."
    )
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Processing payment for check-in {}: amount=${}",
                 request.getCheckInId(), request.getAmount());

        PaymentService.PaymentResult result = paymentService.processPayment(
            request.getCheckInId(),
            request.getAmount(),
            request.getPaymentMethod()
        );

        PaymentResponse response = PaymentResponse.from(result);

        HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.PAYMENT_REQUIRED;

        return ResponseEntity
            .status(status)
            .body(response);
    }
}

