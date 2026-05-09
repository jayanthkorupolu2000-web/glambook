package com.salon.controller;

import com.salon.dto.request.SalonOwnerManagementEditRequest;
import com.salon.dto.response.SalonOwnerManagementResponse;
import com.salon.service.SalonOwnerManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Admin-level Salon Owner management.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin/salon-owners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Salon Owner Management", description = "Admin APIs for editing Salon Owner details")
public class SalonOwnerManagementController {

    private final SalonOwnerManagementService salonOwnerManagementService;

    @PatchMapping("/{id}/edit")
    @Operation(
        summary = "Edit Salon Owner",
        description = "Admin updates ownerName, salonName, and phone. Email and city are read-only.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Salon Owner updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required"),
        @ApiResponse(responseCode = "404", description = "Salon Owner not found")
    })
    public ResponseEntity<SalonOwnerManagementResponse> editSalonOwner(
            @PathVariable Long id,
            @Valid @RequestBody SalonOwnerManagementEditRequest dto) {
        return ResponseEntity.ok(salonOwnerManagementService.updateSalonOwner(id, dto));
    }
}
