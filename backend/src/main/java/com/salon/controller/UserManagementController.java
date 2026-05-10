package com.salon.controller;

import com.salon.dto.request.SalonOwnerEditRequest;
import com.salon.dto.request.UserStatusRequest;
import com.salon.dto.response.SalonOwnerEditResponse;
import com.salon.dto.response.UserStatusResponse;
import com.salon.service.SalonOwnerEditService;
import com.salon.service.SuspensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Admin user suspension, reactivation, and edit APIs")
public class UserManagementController {

    private final SuspensionService suspensionService;
    private final SalonOwnerEditService salonOwnerEditService;

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user status", description = "Admin suspends or reactivates a customer or professional")
    public ResponseEntity<UserStatusResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String userType,
            @Valid @RequestBody UserStatusRequest dto) {
        return ResponseEntity.ok(suspensionService.updateUserStatus(id, userType, dto.getStatus(), dto.getReason()));
    }

    @PatchMapping("/{id}/edit")
    @Operation(summary = "Edit Salon Owner details",
               description = "Admin can update only the name and phone of a Salon Owner. Email, city, and role remain read-only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Salon Owner updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Salon Owner not found")
    })
    public ResponseEntity<SalonOwnerEditResponse> editSalonOwner(
            @PathVariable Long id,
            @Valid @RequestBody SalonOwnerEditRequest dto) {
        return ResponseEntity.ok(salonOwnerEditService.updateSalonOwner(id, dto));
    }
}
