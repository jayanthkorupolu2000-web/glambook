package com.salon.controller;

import com.salon.dto.request.ReviewResponseRequest;
import com.salon.dto.response.ReviewWithResponseDTO;
import com.salon.service.ReviewResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/professionals/{id}/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Responses", description = "Professional review response management")
public class ReviewResponseController {

    private final ReviewResponseService reviewResponseService;

    @PostMapping("/{reviewId}/response")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Respond to a review")
    public ResponseEntity<ReviewWithResponseDTO> respond(
            @PathVariable Long id,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewResponseRequest dto) {
        return ResponseEntity.ok(reviewResponseService.respondToReview(id, reviewId, dto));
    }

    @GetMapping
    @Operation(summary = "Get all reviews for professional (public)")
    public ResponseEntity<List<ReviewWithResponseDTO>> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(reviewResponseService.getReviewsForProfessional(id));
    }
}
