package com.salon.controller;

import com.salon.dto.request.PromotionRequest;
import com.salon.dto.response.PromotionResponse;
import com.salon.service.PromotionService;
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
@RequestMapping("/api/v1/owners/{ownerId}/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Salon promotion management")
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Create a promotion")
    public ResponseEntity<PromotionResponse> create(
            @PathVariable Long ownerId,
            @Valid @RequestBody PromotionRequest dto) {
        dto.setOwnerId(ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createPromotion(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SALON_OWNER','CUSTOMER')")
    @Operation(summary = "Get promotions for owner")
    public ResponseEntity<List<PromotionResponse>> getPromotions(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(promotionService.getActivePromotionsByOwner(ownerId));
        }
        return ResponseEntity.ok(promotionService.getPromotionsByOwner(ownerId));
    }
}
