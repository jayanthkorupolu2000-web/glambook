package com.salon.controller;

import com.salon.dto.response.LoyaltyResponse;
import com.salon.dto.response.LoyaltyTransactionResponse;
import com.salon.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/loyalty")
@RequiredArgsConstructor
@Tag(name = "Customer Loyalty", description = "Customer loyalty points, tiers and redemption")
public class CustomerLoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get loyalty summary — total points, tier, benefits, early-access services")
    public ResponseEntity<LoyaltyResponse> getSummary(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getSummary(customerId));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get full earn/redeem transaction history")
    public ResponseEntity<List<LoyaltyTransactionResponse>> getTransactions(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getTransactions(customerId));
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Redeem points for a discount (multiples of 100; 100 pts = ₹10)")
    public ResponseEntity<LoyaltyResponse> redeem(
            @PathVariable Long customerId,
            @RequestBody Map<String, Integer> body) {
        int points = body.getOrDefault("points", 0);
        return ResponseEntity.ok(loyaltyService.redeemPoints(customerId, points));
    }
}
