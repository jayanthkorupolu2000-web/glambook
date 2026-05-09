package com.salon.controller;

import com.salon.dto.response.AdminNotificationResponse;
import com.salon.service.AdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Notifications", description = "Admin notification management")
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    @Operation(summary = "Get all admin notifications ordered by createdAt descending")
    public ResponseEntity<List<AdminNotificationResponse>> getAll() {
        return ResponseEntity.ok(adminNotificationService.getAll());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark an admin notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        adminNotificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
