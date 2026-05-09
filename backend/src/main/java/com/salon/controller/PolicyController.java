package com.salon.controller;

import com.salon.dto.request.PolicyRequest;
import com.salon.dto.response.PolicyResponse;
import com.salon.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Policy", description = "Policy publishing and retrieval APIs")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping("/admin/policy")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish a policy", description = "Admin publishes a new policy")
    public ResponseEntity<PolicyResponse> publishPolicy(
            @RequestParam Long adminId,
            @Valid @RequestBody PolicyRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.publishPolicy(adminId, dto));
    }

    @GetMapping("/policy/latest")
    @Operation(summary = "Get latest policy", description = "Returns the most recently published policy")
    public ResponseEntity<PolicyResponse> getLatestPolicy() {
        return ResponseEntity.ok(policyService.getLatestPolicy());
    }

    @GetMapping("/admin/policy")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all policies", description = "Admin views all published policies")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }
}
