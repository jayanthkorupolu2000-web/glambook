package com.salon.controller;

import com.salon.exception.UnauthorizedException;
import com.salon.security.JwtUtil;
import com.salon.service.PayLaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/pay-later")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Pay Later", description = "Pay Later eligibility, opt-in, and settlement")
@SecurityRequirement(name = "bearerAuth")
public class PayLaterController {

    private final PayLaterService payLaterService;
    private final JwtUtil jwtUtil;

    /** Check if the customer is eligible for Pay Later on a specific appointment */
    @GetMapping("/eligibility/{appointmentId}")
    @Operation(summary = "Check Pay Later eligibility")
    public ResponseEntity<Map<String, Object>> checkEligibility(
            @PathVariable Long appointmentId,
            HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.ok(payLaterService.checkEligibility(customerId, appointmentId));
    }

    /** Opt in to Pay Later — creates a PAY_LATER_PENDING payment record */
    @PostMapping("/opt-in/{appointmentId}")
    @Operation(summary = "Opt in to Pay Later for an appointment")
    public ResponseEntity<Map<String, Object>> optIn(
            @PathVariable Long appointmentId,
            HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.ok(payLaterService.optIn(customerId, appointmentId));
    }

    /** Settle a Pay Later payment (pay the pending amount) */
    @PostMapping("/settle/{appointmentId}")
    @Operation(summary = "Settle a Pay Later payment")
    public ResponseEntity<Map<String, Object>> settle(
            @PathVariable Long appointmentId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        String method = (String) body.getOrDefault("method", "CASH");
        BigDecimal walletUsed = BigDecimal.ZERO;
        Object w = body.get("walletAmountUsed");
        if (w != null) {
            try { walletUsed = new BigDecimal(w.toString()); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(payLaterService.settle(customerId, appointmentId, method, walletUsed));
    }

    private Long extractCustomerId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new UnauthorizedException("Missing or invalid authorization header");
        return jwtUtil.extractUserId(auth.substring(7));
    }
}
