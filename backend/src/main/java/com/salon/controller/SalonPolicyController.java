package com.salon.controller;

import com.salon.dto.request.SalonPolicyRequest;
import com.salon.dto.response.SalonPolicyResponse;
import com.salon.service.SalonPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Salon Policies", description = "Salon-level policy management")
public class SalonPolicyController {

    private final SalonPolicyService policyService;

    // ── Owner-scoped endpoints ────────────────────────────────────────────────

    @PostMapping("/api/v1/owners/{ownerId}/policies")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Publish a salon policy — notifies all mapped professionals")
    public ResponseEntity<SalonPolicyResponse> publish(
            @PathVariable Long ownerId,
            @Valid @RequestBody SalonPolicyRequest dto) {
        dto.setOwnerId(ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.publishPolicy(dto));
    }

    @GetMapping("/api/v1/owners/{ownerId}/policies")
    @Operation(summary = "Get all policies published by this salon owner")
    public ResponseEntity<List<SalonPolicyResponse>> getAll(@PathVariable Long ownerId) {
        return ResponseEntity.ok(policyService.getPoliciesByOwner(ownerId));
    }

    @GetMapping("/api/v1/owners/{ownerId}/policies/latest")
    @Operation(summary = "Get the most recent policy for this salon owner")
    public ResponseEntity<SalonPolicyResponse> getLatest(@PathVariable Long ownerId) {
        return ResponseEntity.ok(policyService.getLatestPolicyByOwner(ownerId));
    }

    // ── City-scoped endpoint (used by professionals) ──────────────────────────

    @GetMapping("/api/v1/policies/city/{city}")
    @Operation(
        summary = "Get all policies for a city",
        description = "Returns policies published by the salon owner of the given city. " +
                      "Used by professionals to view their salon owner's policies."
    )
    public ResponseEntity<List<SalonPolicyResponse>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(policyService.getPoliciesByCity(city));
    }
}
