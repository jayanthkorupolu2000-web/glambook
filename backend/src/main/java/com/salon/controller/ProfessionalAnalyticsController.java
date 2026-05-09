package com.salon.controller;

import com.salon.dto.response.ProfessionalAnalyticsResponse;
import com.salon.service.ProfessionalAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/professionals/{id}/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROFESSIONAL','SALON_OWNER')")
@Tag(name = "Professional Analytics", description = "Professional performance analytics")
public class ProfessionalAnalyticsController {

    private final ProfessionalAnalyticsService analyticsService;

    @GetMapping
    @Operation(summary = "Get professional analytics")
    public ResponseEntity<ProfessionalAnalyticsResponse> getAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.generateAnalytics(id));
    }
}
