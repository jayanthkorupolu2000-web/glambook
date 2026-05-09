package com.salon.controller;

import com.salon.dto.response.SalonOwnerResponse;
import com.salon.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get all users",
               description = "Returns all customers, salon owners, and professionals",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/owners")
    @Operation(summary = "Get all salon owners",
               description = "Returns all pre-seeded salon owners",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<SalonOwnerResponse>> getAllOwners() {
        return ResponseEntity.ok(adminService.getAllOwners());
    }

    @GetMapping("/reports")
    @Operation(summary = "Get reports",
               description = "Returns summary counts of appointments and payments",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> getReports() {
        return ResponseEntity.ok(adminService.getReports());
    }
}
