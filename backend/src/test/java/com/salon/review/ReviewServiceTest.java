package com.salon.review;

import com.salon.dto.request.ReviewRequest;
import com.salon.dto.response.ReviewResponse;
import com.salon.entity.Customer;
import com.salon.entity.Professional;
import com.salon.entity.Review;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.CustomerRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.ReviewRepository;
import com.salon.service.ReviewService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for ReviewService covering:
 * - Task 10.3: rating outside 1–5 returns validation error; rating within 1–5 is accepted
 * - Task 10.4: created review references correct customerId and professionalId
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private com.salon.repository.AppointmentRepository appointmentRepository;

    @Mock
    private com.salon.repository.ProfessionalNotificationRepository professionalNotificationRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Validator validator;
    private Customer customer;
    private Professional professional;
    private com.salon.entity.Appointment appointment;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        customer = Customer.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .build();

        professional = Professional.builder()
                .id(2L)
                .name("Bob")
                .email("bob@example.com")
                .build();

        appointment = com.salon.entity.Appointment.builder()
                .id(5L)
                .customer(customer)
                .professional(professional)
                .status(com.salon.entity.AppointmentStatus.COMPLETED)
                .build();
    }

    // ── Task 10.3: Bean validation on rating ──────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 100})
    void rating_OutsideRange_ShouldFailBeanValidation(int invalidRating) {
        ReviewRequest request = ReviewRequest.builder()
                .professionalId(2L)
                .qualityRating(invalidRating)
                .timelinessRating(3)
                .professionalismRating(3)
                .build();

        Set<ConstraintViolation<ReviewRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Expected validation failure for rating " + invalidRating);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void rating_WithinRange_ShouldPassBeanValidation(int validRating) {
        ReviewRequest request = ReviewRequest.builder()
                .professionalId(2L)
                .appointmentId(5L)
                .qualityRating(validRating)
                .timelinessRating(validRating)
                .professionalismRating(validRating)
                .build();

        Set<ConstraintViolation<ReviewRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Expected no validation failure for rating " + validRating);
    }

    @Test
    void rating_Null_ShouldFailBeanValidation() {
        ReviewRequest request = ReviewRequest.builder()
                .professionalId(2L)
                .qualityRating(null)
                .timelinessRating(null)
                .professionalismRating(null)
                .build();

        Set<ConstraintViolation<ReviewRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Expected validation failure for null rating");
    }

    // ── Task 10.4: created review references correct customerId and professionalId ──

    @Test
    void createReview_ValidRequest_ShouldReferenceCorrectCustomerAndProfessional() {
        // Given
        ReviewRequest request = ReviewRequest.builder()
                .professionalId(2L)
                .appointmentId(5L)
                .qualityRating(4)
                .timelinessRating(4)
                .professionalismRating(4)
                .comment("Great service!")
                .build();

        Review savedReview = Review.builder()
                .id(10L)
                .customer(customer)
                .professional(professional)
                .rating(4)
                .qualityRating(4)
                .timelinessRating(4)
                .professionalismRating(4)
                .comment("Great service!")
                .createdAt(LocalDateTime.now())
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(2L)).thenReturn(Optional.of(professional));
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(1L, 5L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // When
        ReviewResponse response = reviewService.createReview(1L, request, null);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getCustomerId(), "Review must reference the correct customerId");
        assertEquals(2L, response.getProfessionalId(), "Review must reference the correct professionalId");
        assertEquals(4, response.getRating());
        assertEquals("Great service!", response.getComment());
        assertEquals("Alice", response.getCustomerName());
    }

    @Test
    void createReview_CustomerNotFound_ShouldThrowResourceNotFoundException() {
        ReviewRequest request = ReviewRequest.builder()
                .professionalId(2L)
                .appointmentId(5L)
                .qualityRating(3)
                .timelinessRating(3)
                .professionalismRating(3)
                .build();

        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(99L, request, null));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_ProfessionalNotFound_ShouldThrowResourceNotFoundException() {
        ReviewRequest request = ReviewRequest.builder()
                .professionalId(99L)
                .appointmentId(5L)
                .qualityRating(3)
                .timelinessRating(3)
                .professionalismRating(3)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(1L, request, null));

        verify(reviewRepository, never()).save(any());
    }

    // ── Task 10.2: getReviewsByProfessional returns list and average rating ───

    @Test
    void getReviewsByProfessional_ShouldReturnReviewsAndAverageRating() {
        Review r1 = Review.builder().id(1L).customer(customer).professional(professional)
                .rating(4).comment("Good").createdAt(LocalDateTime.now()).build();
        Review r2 = Review.builder().id(2L).customer(customer).professional(professional)
                .rating(2).comment("OK").createdAt(LocalDateTime.now()).build();

        when(professionalRepository.existsById(2L)).thenReturn(true);
        when(reviewRepository.findByProfessionalId(2L)).thenReturn(List.of(r1, r2));

        Map<String, Object> result = reviewService.getReviewsByProfessional(2L);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<ReviewResponse> reviews = (List<ReviewResponse>) result.get("reviews");
        assertEquals(2, reviews.size());
        assertEquals(3.0, (Double) result.get("averageRating"), 0.001);
    }

    @Test
    void getReviewsByProfessional_NoReviews_ShouldReturnEmptyListAndZeroAverage() {
        when(professionalRepository.existsById(2L)).thenReturn(true);
        when(reviewRepository.findByProfessionalId(2L)).thenReturn(List.of());

        Map<String, Object> result = reviewService.getReviewsByProfessional(2L);

        @SuppressWarnings("unchecked")
        List<ReviewResponse> reviews = (List<ReviewResponse>) result.get("reviews");
        assertTrue(reviews.isEmpty());
        assertEquals(0.0, (Double) result.get("averageRating"), 0.001);
    }

    @Test
    void getReviewsByProfessional_ProfessionalNotFound_ShouldThrowResourceNotFoundException() {
        when(professionalRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getReviewsByProfessional(99L));
    }
}
