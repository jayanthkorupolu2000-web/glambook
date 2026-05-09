package com.salon.controller;

import com.salon.dto.request.ComplaintRequest;
import com.salon.dto.request.MediationRequest;
import com.salon.dto.response.ComplaintResponse;
import com.salon.service.ComplaintService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Complaint management APIs")
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping("/complaints")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Submit a complaint", description = "Customer submits a complaint against a professional")
    public ResponseEntity<ComplaintResponse> createComplaint(@Valid @RequestBody ComplaintRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.createComplaint(dto));
    }

    @GetMapping("/admin/complaints")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all complaints", description = "Admin views all complaints, optionally filtered by status")
    public ResponseEntity<List<ComplaintResponse>> getComplaints(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(complaintService.getComplaintsByStatus(status));
        }
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PatchMapping("/admin/complaints/{id}/forward")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Forward complaint", description = "Admin forwards complaint to salon owner")
    public ResponseEntity<ComplaintResponse> forwardComplaint(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.forwardComplaint(id));
    }

    @PatchMapping("/admin/complaints/{id}/mediate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mediate complaint", description = "Admin resolves complaint with resolution notes")
    public ResponseEntity<ComplaintResponse> mediateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody MediationRequest dto) {
        return ResponseEntity.ok(complaintService.mediateComplaint(id, dto));
    }
}
