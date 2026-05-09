package com.salon.controller;

import com.salon.entity.WalletTransaction;
import com.salon.exception.UnauthorizedException;
import com.salon.security.JwtUtil;
import com.salon.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Customer wallet balance and transaction history")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;
    private final JwtUtil jwtUtil;

    /** GET /api/wallet/balance → { balance, currency } */
    @GetMapping("/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get wallet balance for the logged-in customer")
    public ResponseEntity<Map<String, Object>> getBalance(HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        BigDecimal balance = walletService.getBalance(customerId);
        return ResponseEntity.ok(Map.of(
                "balance", balance,
                "currency", "INR"
        ));
    }

    /** GET /api/wallet/transactions → array of transaction records */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get wallet transaction history for the logged-in customer")
    public ResponseEntity<List<Map<String, Object>>> getTransactions(HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        List<WalletTransaction> txns = walletService.getTransactions(customerId);
        List<Map<String, Object>> result = txns.stream().map(t -> Map.<String, Object>of(
                "id",          t.getId(),
                "type",        t.getType(),
                "amount",      t.getAmount(),
                "source",      t.getSource(),
                "description", t.getDescription() != null ? t.getDescription() : "",
                "createdAt",   t.getCreatedAt() != null ? t.getCreatedAt().toString() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private Long extractCustomerId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new UnauthorizedException("Missing or invalid authorization header");
        return jwtUtil.extractUserId(auth.substring(7));
    }
}
