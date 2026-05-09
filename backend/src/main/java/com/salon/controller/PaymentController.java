package com.salon.controller;

import com.salon.dto.request.PaymentRequest;
import com.salon.dto.response.PaymentResponse;
import com.salon.exception.UnauthorizedException;
import com.salon.security.JwtUtil;
import com.salon.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Process payment for an appointment")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid authorization header");
        }
        Long customerId = jwtUtil.extractUserId(authHeader.substring(7));

        PaymentResponse response = paymentService.processPayment(request, customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get payment details for an appointment")
    public ResponseEntity<PaymentResponse> getPaymentByAppointmentId(@PathVariable Long appointmentId) {
        PaymentResponse response = paymentService.getPaymentByAppointmentId(appointmentId);
        return ResponseEntity.ok(response);
    }
}
