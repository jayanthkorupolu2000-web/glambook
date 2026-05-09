package com.salon.controller;

import com.salon.dto.request.ConsultationReplyRequest;
import com.salon.dto.request.ConsultationRequest;
import com.salon.dto.response.ConsultationResponse;
import com.salon.entity.ConsultationStatus;
import com.salon.service.ConsultationService;
import com.salon.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Consultations")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Get professionals matching a consultation topic")
    @GetMapping("/api/v1/consultations/professionals")
    public ResponseEntity<List<com.salon.dto.response.ProfessionalResponse>> getProfessionalsByTopic(
            @RequestParam(defaultValue = "GENERAL") String topic) {
        return ResponseEntity.ok(consultationService.getProfessionalsByTopic(topic));
    }

    @Operation(summary = "Create consultation")
    @PostMapping(value = "/api/v1/customers/{customerId}/consultations",
                 consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ConsultationResponse> create(
            @PathVariable Long customerId,
            @RequestPart("data") @Valid ConsultationRequest req,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        req.setCustomerId(customerId);
        ConsultationResponse created = consultationService.create(req);
        // Attach photo if provided
        if (photo != null && !photo.isEmpty()) {
            String storedPath = fileStorageService.storePhoto(photo, "consultations");
            String apiUrl = storedPath.replaceFirst("^/uploads/", "/api/v1/files/");
            consultationService.attachPhoto(created.getId(), apiUrl);
            created.setPhotoUrl(apiUrl);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get customer consultations")
    @GetMapping("/api/v1/customers/{customerId}/consultations")
    public ResponseEntity<List<ConsultationResponse>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(consultationService.getByCustomer(customerId));
    }

    @Operation(summary = "Get professional consultations")
    @GetMapping("/api/v1/professionals/{professionalId}/consultations")
    public ResponseEntity<List<ConsultationResponse>> getByProfessional(
            @PathVariable Long professionalId,
            @RequestParam(required = false) ConsultationStatus status) {
        return ResponseEntity.ok(consultationService.getByProfessional(professionalId, status));
    }

    @Operation(summary = "Professional replies to consultation")
    @PatchMapping("/api/v1/professionals/{professionalId}/consultations/{consultationId}/reply")
    public ResponseEntity<ConsultationResponse> reply(
            @PathVariable Long professionalId,
            @PathVariable Long consultationId,
            @Valid @RequestBody ConsultationReplyRequest req) {
        return ResponseEntity.ok(consultationService.reply(professionalId, consultationId, req));
    }

    @Operation(summary = "Owner view consultations")
    @GetMapping("/api/v1/owners/{ownerId}/consultations")
    public ResponseEntity<List<ConsultationResponse>> getByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(consultationService.getBySalonOwner(ownerId));
    }

    @Operation(summary = "Admin view all consultations")
    @GetMapping("/api/v1/admin/consultations")
    public ResponseEntity<List<ConsultationResponse>> getAll() {
        return ResponseEntity.ok(consultationService.getAll());
    }

    @Operation(summary = "Upload photo for a consultation")
    @PostMapping(value = "/api/v1/consultations/{consultationId}/photo",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable Long consultationId,
            @RequestParam("file") MultipartFile file) {
        // Store file and get path like /uploads/consultations/uuid.jpg
        String storedPath = fileStorageService.storePhoto(file, "consultations");
        // Convert to a URL served by FileController: /api/v1/files/consultations/uuid.jpg
        // storedPath = /uploads/consultations/uuid.jpg → strip /uploads/ prefix
        String apiUrl = storedPath.replaceFirst("^/uploads/", "/api/v1/files/");
        consultationService.attachPhoto(consultationId, apiUrl);
        return ResponseEntity.ok(Map.of("photoUrl", apiUrl));
    }
}
