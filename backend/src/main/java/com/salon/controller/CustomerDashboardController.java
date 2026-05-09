package com.salon.controller;

import com.salon.dto.response.CustomerDashboardResponse;
import com.salon.service.CustomerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers/{id}/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Dashboard", description = "Customer dashboard aggregation")
public class CustomerDashboardController {

    private final CustomerDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get customer dashboard")
    public ResponseEntity<CustomerDashboardResponse> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardService.getDashboard(id));
    }
}
