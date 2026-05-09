package com.salon.controller;

import com.salon.dto.response.OwnerReportResponse;
import com.salon.service.OwnerReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/owners/{ownerId}/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Owner Reports", description = "Salon owner analytics and reports")
public class OwnerReportController {

    private final OwnerReportService reportService;

    @GetMapping
    @Operation(summary = "Generate owner report")
    public ResponseEntity<OwnerReportResponse> getReport(@PathVariable Long ownerId) {
        return ResponseEntity.ok(reportService.generateReport(ownerId));
    }
}
