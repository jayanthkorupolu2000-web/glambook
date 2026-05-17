package com.salon.review;

import com.salon.dto.request.ReviewRequest;
import com.salon.dto.request.ReviewUpdateRequest;
import com.salon.dto.response.ReviewResponse;
import com.salon.entity.*;
import com.salon.exception.DuplicateReviewException;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.UnauthorizedException;
import com.salon.repository.*;
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
 * Updated ReviewServiceTest — aligned with the current ReviewService which:
 * - Computes overall rating as average of quality/timeliness/professionalism
 * - Validates appointment status (COMPLETED or CONFIRMED only)
 * - Checks for duplicate reviews per appointment
 * - Sends a ProfessionalNotification on create
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProfessionalNotificationRepository professionalNotificationRepository;

    @InjectMocks private ReviewService reviewService;

    private Validator validator;
    private Customer customer;
    private Professional professional;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        customer = Customer.builder().id(1L).name("Alice").email("alice@gmail.com").build();
        professional = Professional.builder().id(2L).name("Bob").email("bob@gmail.com").build();
        appointment = Appointment.builder()
                .id(5L).customer(customer).professional(professional)
                .status(AppointmentStatus.COMPLETED).build();
    }

    // ── Bean validation ───────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 100})
    void rating_OutsideRange_ShouldFailBeanValidation(int invalid) {
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L)
                .qualityRating(invalid)
                .timelinessRating(3)
                .professionalismRating(3)
                .build();

        Set<ConstraintViolation<ReviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Expected violation for rating=" + invalid);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void rating_WithinRange_ShouldPassBeanValidation(int valid) {
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L).appointmentId(5L)
                .qualityRating(valid).timelinessRating(valid).professionalismRating(valid)
                .build();

        Set<ConstraintViolation<ReviewRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Expected no violation for rating=" + valid);
    }

    @Test
    void rating_Null_ShouldFailBeanValidation() {
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L)
                .qualityRating(null).timelinessRating(null).professionalismRating(null)
                .build();

        Set<ConstraintViolation<ReviewRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    // ── createReview ──────────────────────────────────────────────────────────

    @Test
    void createReview_Valid_ShouldReturnCorrectCustomerAndProfessional() {
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L).appointmentId(5L)
                .qualityRating(4).timelinessRating(4).professionalismRating(4)
                .comment("Great service!")
                .build();

        Review saved = Review.builder()
                .id(10L).customer(customer).professional(professional)
                .appointment(appointment)
                .rating(4).qualityRating(4).timelinessRating(4).professionalismRating(4)
                .comment("Great service!").photos(List.of())
                .status(ReviewStatus.ACTIVE).createdAt(LocalDateTime.now()).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(2L)).thenReturn(Optional.of(professional));
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(1L, 5L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);
        when(professionalNotificationRepository.save(any())).thenReturn(null);

        ReviewResponse response = reviewService.createReview(1L, req, null);

        assertNotNull(response);
        assertEquals(1L, response.getCustomerId());
        assertEquals(2L, response.getProfessionalId());
        assertEquals(4, response.getRating());
        assertEquals("Great service!", response.getComment());
        assertEquals("Alice", response.getCustomerName());
        verify(professionalNotificationRepository).save(any());
    }

    @Test
    void createReview_OverallRating_ShouldBeAverageOfThreeCriteria() {
        // quality=5, timeliness=3, professionalism=4 → avg=4
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L).appointmentId(5L)
                .qualityRating(5).timelinessRating(3).professionalismRating(4)
                .build();

        Review saved = Review.builder()
                .id(11L).customer(customer).professional(professional)
                .appointment(appointment)
                .rating(4).qualityRating(5).timelinessRating(3).professionalismRating(4)
                .photos(List.of()).status(ReviewStatus.ACTIVE).createdAt(LocalDateTime.now()).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(2L)).thenReturn(Optional.of(professional));
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(1L, 5L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);
        when(professionalNotificationRepository.save(any())).thenReturn(null);

        ReviewResponse response = reviewService.createReview(1L, req, null);

        assertEquals(4, response.getRating());
    }

    @Test
    void createReview_CustomerNotFound_ShouldThrow() {
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L).appointmentId(5L)
                .qualityRating(3).timelinessRating(3).professionalismRating(3).build();

        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(99L, req, null));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_ProfessionalNotFound_ShouldThrow() {
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(99L).appointmentId(5L)
                .qualityRating(3).timelinessRating(3).professionalismRating(3).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(1L, req, null));
    }

    @Test
    void createReview_AppointmentNotFound_ShouldThrow() {
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L).appointmentId(99L)
                .qualityRating(3).timelinessRating(3).professionalismRating(3).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(2L)).thenReturn(Optional.of(professional));
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(1L, req, null));
    }

    @Test
    void createReview_AppointmentPending_ShouldThrowInvalidOperationException() {
        appointment.setStatus(AppointmentStatus.PENDING);
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L).appointmentId(5L)
                .qualityRating(3).timelinessRating(3).professionalismRating(3).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(2L)).thenReturn(Optional.of(professional));
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));

        assertThrows(InvalidOperationException.class,
                () -> reviewService.createReview(1L, req, null));
    }

    @Test
    void createReview_DuplicateReview_ShouldThrowDuplicateReviewException() {
        ReviewRequest req = ReviewRequest.builder()
                .professionalId(2L).appointmentId(5L)
                .qualityRating(3).timelinessRating(3).professionalismRating(3).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(2L)).thenReturn(Optional.of(professional));
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(1L, 5L)).thenReturn(true);

        assertThrows(DuplicateReviewException.class,
                () -> reviewService.createReview(1L, req, null));
    }

    // ── updateReview ──────────────────────────────────────────────────────────

    @Test
    void updateReview_ValidOwner_ShouldUpdateCommentAndRatings() {
        Review existing = Review.builder()
                .id(10L).customer(customer).professional(professional)
                .appointment(appointment)
                .rating(3).qualityRating(3).timelinessRating(3).professionalismRating(3)
                .photos(List.of()).status(ReviewStatus.ACTIVE).build();

        ReviewUpdateRequest dto = ReviewUpdateRequest.builder()
                .comment("Updated comment")
                .qualityRating(5).timelinessRating(5).professionalismRating(5)
                .build();

        Review updated = Review.builder()
                .id(10L).customer(customer).professional(professional)
                .appointment(appointment)
                .rating(5).qualityRating(5).timelinessRating(5).professionalismRating(5)
                .comment("Updated comment").photos(List.of())
                .status(ReviewStatus.UPDATED).createdAt(LocalDateTime.now()).build();

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenReturn(updated);

        ReviewResponse response = reviewService.updateReview(1L, 10L, dto, null);

        assertNotNull(response);
        assertEquals("Updated comment", response.getComment());
        assertEquals(5, response.getRating());
        assertEquals("UPDATED", response.getStatus());
    }

    @Test
    void updateReview_WrongCustomer_ShouldThrowUnauthorizedException() {
        Review existing = Review.builder()
                .id(10L).customer(customer).professional(professional)
                .appointment(appointment).rating(3)
                .qualityRating(3).timelinessRating(3).professionalismRating(3)
                .photos(List.of()).build();

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));

        ReviewUpdateRequest dto = ReviewUpdateRequest.builder().comment("Hack").build();

        assertThrows(UnauthorizedException.class,
                () -> reviewService.updateReview(99L, 10L, dto, null));
    }

    @Test
    void updateReview_ReviewNotFound_ShouldThrow() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.updateReview(1L, 99L,
                        ReviewUpdateRequest.builder().build(), null));
    }

    // ── getReviewsByProfessional ──────────────────────────────────────────────

    @Test
    void getReviewsByProfessional_ShouldReturnReviewsAndAverages() {
        Review r1 = Review.builder().id(1L).customer(customer).professional(professional)
                .rating(4).qualityRating(4).timelinessRating(4).professionalismRating(4)
                .photos(List.of()).createdAt(LocalDateTime.now()).build();
        Review r2 = Review.builder().id(2L).customer(customer).professional(professional)
                .rating(2).qualityRating(2).timelinessRating(2).professionalismRating(2)
                .photos(List.of()).createdAt(LocalDateTime.now()).build();

        when(professionalRepository.existsById(2L)).thenReturn(true);
        when(reviewRepository.findByProfessionalId(2L)).thenReturn(List.of(r1, r2));

        Map<String, Object> result = reviewService.getReviewsByProfessional(2L);

        @SuppressWarnings("unchecked")
        List<ReviewResponse> reviews = (List<ReviewResponse>) result.get("reviews");
        assertEquals(2, reviews.size());
        assertEquals(3.0, (Double) result.get("averageRating"), 0.001);
    }

    @Test
    void getReviewsByProfessional_Empty_ShouldReturnZeroAverage() {
        when(professionalRepository.existsById(2L)).thenReturn(true);
        when(reviewRepository.findByProfessionalId(2L)).thenReturn(List.of());

        Map<String, Object> result = reviewService.getReviewsByProfessional(2L);

        @SuppressWarnings("unchecked")
        List<ReviewResponse> reviews = (List<ReviewResponse>) result.get("reviews");
        assertTrue(reviews.isEmpty());
        assertEquals(0.0, (Double) result.get("averageRating"), 0.001);
    }

    @Test
    void getReviewsByProfessional_ProfessionalNotFound_ShouldThrow() {
        when(professionalRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getReviewsByProfessional(99L));
    }

    // ── getReviewsByCustomer ──────────────────────────────────────────────────

    @Test
    void getReviewsByCustomer_ShouldReturnList() {
        Review r = Review.builder().id(1L).customer(customer).professional(professional)
                .rating(5).photos(List.of()).createdAt(LocalDateTime.now()).build();

        when(reviewRepository.findByCustomerId(1L)).thenReturn(List.of(r));

        List<ReviewResponse> result = reviewService.getReviewsByCustomer(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getCustomerId());
    }

    // ── existsByAppointmentId ─────────────────────────────────────────────────

    @Test
    void existsByAppointmentId_ShouldDelegateToRepository() {
        when(reviewRepository.existsByAppointmentId(5L)).thenReturn(true);

        assertTrue(reviewService.existsByAppointmentId(5L));
    }
}
