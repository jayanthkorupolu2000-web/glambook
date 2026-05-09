package com.salon.controller;

import com.salon.dto.request.BeautyProfileRequest;
import com.salon.dto.response.BeautyProfileResponse;
import com.salon.service.BeautyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Handles GET and PUT for a customer's beauty profile.
 * Mounted at both /api/v1/customers/{id}/beauty-profile (new standard)
 * and /api/customers/{id}/beauty-profile (legacy — frontend currently uses this).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Beauty Profile", description = "Customer beauty profile management")
public class BeautyProfileController {

    private final BeautyProfileService beautyProfileService;

    // ── /api/v1/ prefix (standard) ────────────────────────────────────────────

    @GetMapping("/api/v1/customers/{customerId}/beauty-profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer's beauty profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile returned (empty object if not yet created)"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<BeautyProfileResponse> getProfile(@PathVariable Long customerId) {
        return ResponseEntity.ok(beautyProfileService.getProfile(customerId));
    }

    @PutMapping("/api/v1/customers/{customerId}/beauty-profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Save (create or update) customer's beauty profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile saved"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<BeautyProfileResponse> saveProfile(
            @PathVariable Long customerId,
            @RequestBody BeautyProfileRequest dto) {
        return ResponseEntity.ok(beautyProfileService.saveProfile(customerId, dto));
    }

    // ── /api/ prefix (legacy — matches what the frontend currently calls) ─────

    @GetMapping("/api/customers/{customerId}/beauty-profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get beauty profile (legacy path)")
    public ResponseEntity<BeautyProfileResponse> getProfileLegacy(@PathVariable Long customerId) {
        return ResponseEntity.ok(beautyProfileService.getProfile(customerId));
    }

    @PutMapping("/api/customers/{customerId}/beauty-profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Save beauty profile (legacy path)")
    public ResponseEntity<BeautyProfileResponse> saveProfileLegacy(
            @PathVariable Long customerId,
            @RequestBody BeautyProfileRequest dto) {
        return ResponseEntity.ok(beautyProfileService.saveProfile(customerId, dto));
    }
}
