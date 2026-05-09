package com.salon.controller;

import com.salon.dto.request.UpdateProfileRequest;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.service.ProfessionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
@Tag(name = "Professional", description = "Professional management APIs")
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @GetMapping
    @Operation(summary = "Get professionals by city")
    public ResponseEntity<Page<ProfessionalResponse>> getProfessionalsByCity(
            @RequestParam String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(professionalService.getProfessionalsByCity(city, pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all professionals as a flat list")
    public ResponseEntity<List<ProfessionalResponse>> getAllProfessionals() {
        return ResponseEntity.ok(professionalService.getAllProfessionals());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get professional by ID", description = "Retrieve a single professional profile by ID")
    public ResponseEntity<ProfessionalResponse> getProfessionalById(@PathVariable Long id) {
        ProfessionalResponse professional = professionalService.getProfessionalById(id);
        return ResponseEntity.ok(professional);
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Update professional profile", 
               description = "Allow professional to update their own profile (name, specialization, experience)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProfessionalResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        
        ProfessionalResponse updatedProfessional = professionalService.updateProfile(id, request, httpRequest);
        return ResponseEntity.ok(updatedProfessional);
    }
}
