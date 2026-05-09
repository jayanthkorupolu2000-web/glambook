package com.salon.controller;

import com.salon.dto.response.OwnerNotificationResponse;
import com.salon.service.OwnerNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/owners/{ownerId}/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Notifications", description = "Owner notification management")
public class NotificationController {

    private final OwnerNotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for owner")
    public ResponseEntity<List<OwnerNotificationResponse>> getAll(@PathVariable Long ownerId) {
        return ResponseEntity.ok(notificationService.getNotificationsForOwner(ownerId));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> unreadCount(@PathVariable Long ownerId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(ownerId)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long ownerId,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead(@PathVariable Long ownerId) {
        notificationService.markAllAsRead(ownerId);
        return ResponseEntity.ok().build();
    }
}
