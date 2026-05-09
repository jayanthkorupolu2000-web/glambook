package com.salon.controller;

import com.salon.dto.request.ResourceAvailabilityRequest;
import com.salon.dto.request.ResourceRequest;
import com.salon.dto.response.ResourceResponse;
import com.salon.service.ResourceService;
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
@RequestMapping("/api/v1/owners/{ownerId}/resources")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Resources", description = "Salon resource management")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @Operation(summary = "Add a resource")
    public ResponseEntity<ResourceResponse> addResource(
            @PathVariable Long ownerId,
            @Valid @RequestBody ResourceRequest dto) {
        dto.setOwnerId(ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.addResource(dto));
    }

    @GetMapping
    @Operation(summary = "Get all resources for owner")
    public ResponseEntity<List<ResourceResponse>> getResources(@PathVariable Long ownerId) {
        return ResponseEntity.ok(resourceService.getResourcesByOwner(ownerId));
    }

    @PostMapping("/{resourceId}/availability")
    @Operation(summary = "Add availability slot to resource")
    public ResponseEntity<ResourceResponse> addSlot(
            @PathVariable Long ownerId,
            @PathVariable Long resourceId,
            @Valid @RequestBody ResourceAvailabilityRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.addAvailabilitySlot(resourceId, dto));
    }

    @PatchMapping("/{resourceId}/toggle")
    @Operation(summary = "Toggle resource availability")
    public ResponseEntity<ResourceResponse> toggle(
            @PathVariable Long ownerId,
            @PathVariable Long resourceId,
            @RequestParam boolean available) {
        return ResponseEntity.ok(resourceService.updateResourceAvailability(resourceId, available));
    }
}
