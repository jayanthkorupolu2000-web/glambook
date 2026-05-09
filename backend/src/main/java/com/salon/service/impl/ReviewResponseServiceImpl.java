package com.salon.service.impl;

import com.salon.dto.request.ReviewResponseRequest;
import com.salon.dto.response.ReviewWithResponseDTO;
import com.salon.entity.Review;
import com.salon.exception.ComplaintNotFoundException;
import com.salon.exception.InvalidOperationException;
import com.salon.repository.ReviewRepository;
import com.salon.service.ReviewResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewResponseServiceImpl implements ReviewResponseService {

    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public ReviewWithResponseDTO respondToReview(Long professionalId, Long reviewId, ReviewResponseRequest dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ComplaintNotFoundException("Review not found: " + reviewId));

        if (!review.getProfessional().getId().equals(professionalId)) {
            throw new AccessDeniedException("This review does not belong to your profile");
        }

        if (review.getProfessionalResponse() != null) {
            throw new InvalidOperationException("You have already responded to this review");
        }

        review.setProfessionalResponse(dto.getResponse());
        review.setProfessionalResponseAt(LocalDateTime.now());

        return toResponse(reviewRepository.save(review));
    }

    @Override
    public List<ReviewWithResponseDTO> getReviewsForProfessional(Long professionalId) {
        return reviewRepository.findByProfessionalId(professionalId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ReviewWithResponseDTO toResponse(Review r) {
        ReviewWithResponseDTO res = new ReviewWithResponseDTO();
        res.setId(r.getId());
        res.setCustomerId(r.getCustomer().getId());
        res.setCustomerName(r.getCustomer().getName());
        res.setProfessionalId(r.getProfessional().getId());
        res.setAppointmentId(r.getAppointment() != null ? r.getAppointment().getId() : null);
        res.setRating(r.getRating());
        res.setComment(r.getComment());
        res.setPhotos(r.getPhotos() != null ? r.getPhotos() : new java.util.ArrayList<>());
        res.setStatus(r.getStatus() != null ? r.getStatus().name() : "ACTIVE");
        res.setCreatedAt(r.getCreatedAt());
        res.setUpdatedAt(r.getUpdatedAt());
        res.setProfessionalResponse(r.getProfessionalResponse());
        res.setProfessionalResponseAt(r.getProfessionalResponseAt());
        return res;
    }
}
