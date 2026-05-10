package com.salon.controller;

import com.salon.dto.request.MediationActionRequest;
import com.salon.dto.request.SuspendProfessionalRequest;
import com.salon.dto.response.ComplaintResponse;
import com.salon.service.ComplaintIntegrationService;
import com.salon.service.SalonOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/owners/{ownerId}/complaints")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Owner Complaints", description = "Complaint integration for salon owners")
public class OwnerComplaintController {

    private final ComplaintIntegrationService complaintService;
    private final SalonOwnerService salonOwnerService;

    @GetMapping
    @Operation(summary = "Get complaints for this owner's professionals")
    public ResponseEntity<List<ComplaintResponse>> getComplaints(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "FORWARDED") String status) {
        if ("ALL".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(complaintService.getAllComplaintsForOwner(ownerId));
        }
        return ResponseEntity.ok(complaintService.getForwardedComplaintsForOwner(ownerId));
    }

    @PatchMapping("/{complaintId}/action")
    @Operation(summary = "Log owner corrective action on a complaint")
    public ResponseEntity<ComplaintResponse> logAction(
            @PathVariable Long ownerId,
            @PathVariable Long complaintId,
            @Valid @RequestBody MediationActionRequest dto) {
        return ResponseEntity.ok(complaintService.logOwnerAction(ownerId, complaintId, dto));
    }

    @PatchMapping("/{complaintId}/suspend-professional")
    @Operation(summary = "Suspend the professional linked to a complaint")
    public ResponseEntity<Map<String, String>> suspendProfessional(
            @PathVariable Long ownerId,
            @PathVariable Long complaintId,
            @Valid @RequestBody SuspendProfessionalRequest request) {
        ComplaintResponse complaint = complaintService.getComplaintById(complaintId);
        salonOwnerService.suspendProfessional(ownerId, complaint.getProfessionalId(), request);
        return ResponseEntity.ok(Map.of("message",
                "Professional suspended and complaint action logged."));
    }
}
