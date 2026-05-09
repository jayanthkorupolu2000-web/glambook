package com.salon.controller;

import com.salon.dto.request.GroupBookingRequest;
import com.salon.dto.response.GroupBookingResponse;
import com.salon.service.GroupBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Group Bookings")
public class GroupBookingController {

    private final GroupBookingService groupBookingService;

    // Customer endpoints
    @Operation(summary = "Create group booking")
    @PostMapping("/api/v1/customers/{customerId}/group-bookings")
    public ResponseEntity<GroupBookingResponse> create(
            @PathVariable Long customerId,
            @Valid @RequestBody GroupBookingRequest req) {
        req.setCustomerId(customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(groupBookingService.createGroupBooking(req));
    }

    @Operation(summary = "Get customer group bookings")
    @GetMapping("/api/v1/customers/{customerId}/group-bookings")
    public ResponseEntity<List<GroupBookingResponse>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(groupBookingService.getByCustomer(customerId));
    }

    @Operation(summary = "Cancel group booking")
    @PatchMapping("/api/v1/customers/{customerId}/group-bookings/{groupBookingId}/cancel")
    public ResponseEntity<GroupBookingResponse> cancel(
            @PathVariable Long customerId,
            @PathVariable Long groupBookingId) {
        return ResponseEntity.ok(groupBookingService.cancel(customerId, groupBookingId));
    }

    // Salon Owner endpoints
    @Operation(summary = "Get owner group bookings")
    @GetMapping("/api/v1/owners/{ownerId}/group-bookings")
    public ResponseEntity<List<GroupBookingResponse>> getByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(groupBookingService.getBySalonOwner(ownerId));
    }

    @Operation(summary = "Confirm group booking")
    @PatchMapping("/api/v1/owners/{ownerId}/group-bookings/{groupBookingId}/confirm")
    public ResponseEntity<GroupBookingResponse> confirm(
            @PathVariable Long ownerId,
            @PathVariable Long groupBookingId) {
        return ResponseEntity.ok(groupBookingService.confirm(ownerId, groupBookingId));
    }

    @Operation(summary = "Complete group booking")
    @PatchMapping("/api/v1/owners/{ownerId}/group-bookings/{groupBookingId}/complete")
    public ResponseEntity<GroupBookingResponse> complete(
            @PathVariable Long ownerId,
            @PathVariable Long groupBookingId) {
        return ResponseEntity.ok(groupBookingService.complete(ownerId, groupBookingId));
    }

    // Professional endpoint
    @Operation(summary = "Get professional group bookings")
    @GetMapping("/api/v1/professionals/{professionalId}/group-bookings")
    public ResponseEntity<List<GroupBookingResponse>> getByProfessional(@PathVariable Long professionalId) {
        return ResponseEntity.ok(groupBookingService.getByProfessional(professionalId));
    }

    // Admin endpoint
    @Operation(summary = "Admin: all group bookings")
    @GetMapping("/api/v1/admin/group-bookings")
    public ResponseEntity<List<GroupBookingResponse>> getAll() {
        return ResponseEntity.ok(groupBookingService.getAll());
    }
}
