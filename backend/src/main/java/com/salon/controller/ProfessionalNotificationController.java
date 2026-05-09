package com.salon.controller;

import com.salon.dto.response.ProfessionalNotificationResponse;
import com.salon.service.ProfessionalNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/professionals/{id}/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSIONAL')")
@Tag(name = "Professional Notifications", description = "Professional notification management")
public class ProfessionalNotificationController {

    private final ProfessionalNotificationService notifService;

    @GetMapping
    @Operation(summary = "Get all notifications")
    public ResponseEntity<List<ProfessionalNotificationResponse>> getAll(@PathVariable Long id) {
        return ResponseEntity.ok(notifService.getNotifications(id));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count")
    public ResponseEntity<Map<String, Long>> unreadCount(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("count", notifService.getUnreadCount(id)));
    }

    @PatchMapping("/{notifId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @PathVariable Long notifId) {
        notifService.markAsRead(notifId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all as read")
    public ResponseEntity<Void> markAllRead(@PathVariable Long id) {
        notifService.markAllAsRead(id);
        return ResponseEntity.ok().build();
    }
}
