package com.salon.controller;

import com.salon.dto.request.LoyaltyUpdateRequest;
import com.salon.dto.response.LoyaltyResponse;
import com.salon.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owners/{ownerId}/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Customer loyalty management")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Get all loyalty records for owner")
    public ResponseEntity<List<LoyaltyResponse>> getAll(@PathVariable Long ownerId) {
        return ResponseEntity.ok(loyaltyService.getAllLoyaltyByOwner(ownerId));
    }

    @PatchMapping("/{customerId}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Update customer loyalty points")
    public ResponseEntity<LoyaltyResponse> updatePoints(
            @PathVariable Long ownerId,
            @PathVariable Long customerId,
            @Valid @RequestBody LoyaltyUpdateRequest dto) {
        return ResponseEntity.ok(loyaltyService.updatePoints(customerId, ownerId, dto.getPoints()));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('SALON_OWNER','CUSTOMER')")
    @Operation(summary = "Get loyalty record for a customer")
    public ResponseEntity<LoyaltyResponse> getByCustomer(
            @PathVariable Long ownerId,
            @PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getLoyaltyByCustomer(customerId, ownerId));
    }
}
