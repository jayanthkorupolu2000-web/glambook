package com.salon.controller;

import com.salon.dto.request.ReviewRequest;
import com.salon.dto.request.ReviewUpdateRequest;
import com.salon.dto.response.ReviewResponse;
import com.salon.exception.UnauthorizedException;
import com.salon.security.JwtUtil;
import com.salon.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Review management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtUtil jwtUtil;

    // ── Create review (with optional photos) ─────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a review with optional photos")
    public ResponseEntity<ReviewResponse> createReview(
            @RequestPart("data") @Valid ReviewRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            HttpServletRequest httpRequest) {

        Long customerId = extractCustomerId(httpRequest);
        ReviewResponse response = reviewService.createReview(customerId, request, photos);
        return ResponseEntity.status(201).body(response);
    }

    // ── Also support JSON-only (no photos) for backward compatibility ─────────

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a review (JSON, no photos)")
    public ResponseEntity<ReviewResponse> createReviewJson(
            @Valid @RequestBody ReviewRequest request,
            HttpServletRequest httpRequest) {

        Long customerId = extractCustomerId(httpRequest);
        ReviewResponse response = reviewService.createReview(customerId, request, null);
        return ResponseEntity.status(201).body(response);
    }

    // ── Update review (append photos, edit comment) ───────────────────────────

    @PatchMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update a review — edit comment and/or add more photos")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @RequestPart("data") ReviewUpdateRequest dto,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            HttpServletRequest httpRequest) {

        Long customerId = extractCustomerId(httpRequest);
        return ResponseEntity.ok(reviewService.updateReview(customerId, reviewId, dto, photos));
    }

    @PatchMapping(value = "/{reviewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update a review (JSON, no new photos)")
    public ResponseEntity<ReviewResponse> updateReviewJson(
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequest dto,
            HttpServletRequest httpRequest) {

        Long customerId = extractCustomerId(httpRequest);
        return ResponseEntity.ok(reviewService.updateReview(customerId, reviewId, dto, null));
    }

    // ── Get reviews by professional ───────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get reviews and average rating for a professional")
    public ResponseEntity<Map<String, Object>> getReviewsByProfessional(
            @RequestParam Long professionalId) {
        return ResponseEntity.ok(reviewService.getReviewsByProfessional(professionalId));
    }

    // ── Get reviews by customer ───────────────────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get all reviews written by the logged-in customer")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(HttpServletRequest httpRequest) {
        Long customerId = extractCustomerId(httpRequest);
        return ResponseEntity.ok(reviewService.getReviewsByCustomer(customerId));
    }

    // ── Check if review exists for appointment ────────────────────────────────

    @GetMapping("/exists")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Check if a review exists for a given appointment")
    public ResponseEntity<Map<String, Boolean>> existsByAppointment(@RequestParam Long appointmentId) {
        return ResponseEntity.ok(Map.of("exists", reviewService.existsByAppointmentId(appointmentId)));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long extractCustomerId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new UnauthorizedException("Missing or invalid authorization header");
        return jwtUtil.extractUserId(auth.substring(7));
    }
}
