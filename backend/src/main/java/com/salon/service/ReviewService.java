package com.salon.service;

import com.salon.dto.request.ReviewRequest;
import com.salon.dto.request.ReviewUpdateRequest;
import com.salon.dto.response.ReviewResponse;
import com.salon.entity.*;
import com.salon.exception.DuplicateReviewException;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.UnauthorizedException;
import com.salon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalNotificationRepository professionalNotificationRepository;

    private static final String UPLOAD_DIR = "uploads/reviews/";

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponse createReview(Long customerId, ReviewRequest request,
                                       List<MultipartFile> photoFiles) {
        log.info("Creating review for professional: {} by customer: {} for appointment: {}",
                request.getProfessionalId(), customerId, request.getAppointmentId());

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Professional professional = professionalRepository.findById(request.getProfessionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.COMPLETED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidOperationException("Reviews can only be submitted for confirmed or completed appointments");
        }

        if (reviewRepository.existsByCustomerIdAndAppointmentId(customerId, request.getAppointmentId())) {
            throw new DuplicateReviewException("A review for this appointment already exists");
        }

        List<String> photoPaths = savePhotos(photoFiles);

        Review review = Review.builder()
                .customer(customer)
                .professional(professional)
                .appointment(appointment)
                .rating(request.getRating())
                .comment(request.getComment())
                .photos(photoPaths)
                .status(ReviewStatus.ACTIVE)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Created review id={}", saved.getId());

        // Notify professional
        String comment = request.getComment() != null ? request.getComment() : "";
        String snippet = comment.length() > 100 ? comment.substring(0, 100) : comment;
        String message = customer.getName() + " rated you " + request.getRating() + "/5"
                + (snippet.isEmpty() ? "" : ": " + snippet);

        professionalNotificationRepository.save(ProfessionalNotification.builder()
                .professional(professional)
                .type(ProfessionalNotificationType.NEW_REVIEW)
                .referenceId(saved.getId())
                .message(message)
                .build());

        return toResponse(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponse updateReview(Long customerId, Long reviewId,
                                       ReviewUpdateRequest dto,
                                       List<MultipartFile> newPhotoFiles) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedException("You can only edit your own reviews");
        }

        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            review.setComment(dto.getComment());
        }

        // Append new photos to existing list
        List<String> newPaths = savePhotos(newPhotoFiles);
        if (!newPaths.isEmpty()) {
            List<String> all = new ArrayList<>(review.getPhotos());
            all.addAll(newPaths);
            review.setPhotos(all);
        }

        review.setStatus(ReviewStatus.UPDATED);
        Review saved = reviewRepository.save(review);
        log.info("Updated review id={}", reviewId);
        return toResponse(saved);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean existsByAppointmentId(Long appointmentId) {
        return reviewRepository.existsByAppointmentId(appointmentId);
    }

    public List<ReviewResponse> getReviewsByCustomer(Long customerId) {
        return reviewRepository.findByCustomerId(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Map<String, Object> getReviewsByProfessional(Long professionalId) {
        if (!professionalRepository.existsById(professionalId)) {
            throw new ResourceNotFoundException("Professional not found");
        }
        List<Review> reviews = reviewRepository.findByProfessionalId(professionalId);
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return Map.of(
                "reviews", reviews.stream().map(this::toResponse).collect(Collectors.toList()),
                "averageRating", avg
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> savePhotos(List<MultipartFile> files) {
        List<String> paths = new ArrayList<>();
        if (files == null || files.isEmpty()) return paths;
        try {
            Path dir = Paths.get(UPLOAD_DIR);
            Files.createDirectories(dir);
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                String ext = getExtension(file.getOriginalFilename());
                String filename = UUID.randomUUID() + ext;
                Path dest = dir.resolve(filename);
                file.transferTo(dest.toFile());
                // Store as API path so frontend can fetch via FileController
                paths.add("/api/v1/files/reviews/" + filename);
            }
        } catch (IOException e) {
            log.error("Failed to save review photos: {}", e.getMessage());
        }
        return paths;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .customerId(review.getCustomer().getId())
                .customerName(review.getCustomer().getName())
                .professionalId(review.getProfessional().getId())
                .appointmentId(review.getAppointment() != null ? review.getAppointment().getId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .photos(review.getPhotos())
                .professionalResponse(review.getProfessionalResponse())
                .status(review.getStatus() != null ? review.getStatus().name() : "ACTIVE")
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
