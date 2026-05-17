package com.salon.service;

import com.salon.dto.request.ReviewResponseRequest;
import com.salon.dto.response.ReviewWithResponseDTO;
import com.salon.entity.*;
import com.salon.exception.ComplaintNotFoundException;
import com.salon.exception.InvalidOperationException;
import com.salon.repository.ReviewRepository;
import com.salon.service.impl.ReviewResponseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for ReviewResponseServiceImpl — aligned with actual implementation
 * which only injects ReviewRepository (no ProfessionalRepository).
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceExtendedTest {

    @Mock private ReviewRepository reviewRepository;

    @InjectMocks private ReviewResponseServiceImpl reviewResponseService;

    private Customer customer;
    private Professional professional;
    private Review review;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice").build();
        professional = Professional.builder().id(2L).name("Bob").build();
        review = Review.builder()
                .id(10L).customer(customer).professional(professional)
                .rating(4).qualityRating(4).timelinessRating(4).professionalismRating(4)
                .comment("Good service").photos(List.of())
                .status(ReviewStatus.ACTIVE).createdAt(LocalDateTime.now()).build();
    }

    // ── respondToReview ───────────────────────────────────────────────────────

    @Test
    void respondToReview_Valid_ShouldSetResponseAndTimestamp() {
        ReviewResponseRequest dto = new ReviewResponseRequest();
        dto.setResponse("Thank you for your feedback!");

        Review responded = Review.builder()
                .id(10L).customer(customer).professional(professional)
                .rating(4).qualityRating(4).timelinessRating(4).professionalismRating(4)
                .comment("Good service").photos(List.of())
                .professionalResponse("Thank you for your feedback!")
                .professionalResponseAt(LocalDateTime.now())
                .status(ReviewStatus.ACTIVE).createdAt(LocalDateTime.now()).build();

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(responded);

        ReviewWithResponseDTO result = reviewResponseService.respondToReview(2L, 10L, dto);

        assertNotNull(result);
        assertEquals("Thank you for your feedback!", result.getProfessionalResponse());
        assertNotNull(result.getProfessionalResponseAt());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void respondToReview_ReviewNotFound_ShouldThrowComplaintNotFoundException() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ComplaintNotFoundException.class,
                () -> reviewResponseService.respondToReview(2L, 99L,
                        new ReviewResponseRequest()));
    }

    @Test
    void respondToReview_WrongProfessional_ShouldThrowAccessDeniedException() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        // Professional 99 tries to respond to a review belonging to professional 2
        assertThrows(AccessDeniedException.class,
                () -> reviewResponseService.respondToReview(99L, 10L,
                        new ReviewResponseRequest()));
    }

    @Test
    void respondToReview_AlreadyResponded_ShouldThrowInvalidOperationException() {
        review.setProfessionalResponse("Already replied");
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        assertThrows(InvalidOperationException.class,
                () -> reviewResponseService.respondToReview(2L, 10L,
                        new ReviewResponseRequest()));
    }

    // ── getReviewsForProfessional ─────────────────────────────────────────────

    @Test
    void getReviewsForProfessional_ShouldReturnMappedList() {
        when(reviewRepository.findByProfessionalId(2L)).thenReturn(List.of(review));

        List<ReviewWithResponseDTO> result = reviewResponseService.getReviewsForProfessional(2L);

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getCustomerName());
        assertEquals(2L, result.get(0).getProfessionalId());
        assertEquals(4, result.get(0).getRating());
    }

    @Test
    void getReviewsForProfessional_Empty_ShouldReturnEmptyList() {
        when(reviewRepository.findByProfessionalId(2L)).thenReturn(List.of());

        List<ReviewWithResponseDTO> result = reviewResponseService.getReviewsForProfessional(2L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getReviewsForProfessional_MapsAllFields_ShouldIncludeCommentAndStatus() {
        when(reviewRepository.findByProfessionalId(2L)).thenReturn(List.of(review));

        List<ReviewWithResponseDTO> result = reviewResponseService.getReviewsForProfessional(2L);

        ReviewWithResponseDTO dto = result.get(0);
        assertEquals(10L, dto.getId());
        assertEquals(1L, dto.getCustomerId());
        assertEquals("Good service", dto.getComment());
        assertEquals("ACTIVE", dto.getStatus());
        assertNull(dto.getProfessionalResponse());
    }
}
