package com.salon.payment;

import com.salon.dto.request.PaymentRequest;
import com.salon.dto.response.PaymentResponse;
import com.salon.entity.*;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.ValidationException;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.PaymentRepository;
import com.salon.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private com.salon.repository.ReviewRepository reviewRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Appointment appointment;
    private Service service;
    private PaymentRequest paymentRequest;
    private Payment payment;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        service = Service.builder()
                .id(1L)
                .name("Haircut")
                .price(new BigDecimal("150.00"))
                .build();

        appointment = Appointment.builder()
                .id(1L)
                .service(service)
                .dateTime(LocalDateTime.now().minusHours(1)) // Past appointment
                .build();

        paymentRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CASH)
                .build();

        payment = Payment.builder()
                .id(1L)
                .appointment(appointment)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        paymentResponse = PaymentResponse.builder()
                .id(1L)
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method("CASH")
                .status("PAID")
                .build();
    }

    @Test
    void processPayment_ValidRequest_ShouldCreatePayment() {
        // Given
        Long customerId = 1L;
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(customerId, 1L)).thenReturn(true);
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(modelMapper.map(payment, PaymentResponse.class)).thenReturn(paymentResponse);

        // When
        PaymentResponse result = paymentService.processPayment(paymentRequest, customerId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getAppointmentId());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals("CASH", result.getMethod());
        assertEquals("PAID", result.getStatus());

        verify(appointmentRepository).findById(1L);
        verify(reviewRepository).existsByCustomerIdAndAppointmentId(customerId, 1L);
        verify(paymentRepository).findByAppointmentId(1L);
        verify(paymentRepository).save(any(Payment.class));
        verify(modelMapper).map(payment, PaymentResponse.class);
    }

    @Test
    void processPayment_AppointmentNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Long customerId = 1L;
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            paymentService.processPayment(paymentRequest, customerId));

        verify(appointmentRepository).findById(1L);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void processPayment_AmountMismatch_ShouldThrowValidationException() {
        // Given
        Long customerId = 1L;
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("100.00")) // Different from service price
                .method(PaymentMethod.CASH)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(customerId, 1L)).thenReturn(true);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            paymentService.processPayment(invalidRequest, customerId));

        assertTrue(exception.getMessage().contains("Payment amount must equal the service price"));

        verify(appointmentRepository).findById(1L);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void processPayment_PaymentAlreadyExists_ShouldThrowValidationException() {
        // Given
        Long customerId = 1L;
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(customerId, 1L)).thenReturn(true);
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.of(payment));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            paymentService.processPayment(paymentRequest, customerId));

        assertEquals("Payment already exists for this appointment", exception.getMessage());

        verify(appointmentRepository).findById(1L);
        verify(paymentRepository).findByAppointmentId(1L);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_WithCashMethod_ShouldSucceed() {
        // Given
        Long customerId = 1L;
        PaymentRequest cashRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CASH)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(customerId, 1L)).thenReturn(true);
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(modelMapper.map(payment, PaymentResponse.class)).thenReturn(paymentResponse);

        // When
        PaymentResponse result = paymentService.processPayment(cashRequest, customerId);

        // Then
        assertNotNull(result);
        assertEquals("CASH", result.getMethod());
    }

    @Test
    void processPayment_WithCardMethod_ShouldSucceed() {
        // Given
        Long customerId = 1L;
        PaymentRequest cardRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CARD)
                .build();

        Payment cardPayment = Payment.builder()
                .id(1L)
                .appointment(appointment)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        PaymentResponse cardResponse = PaymentResponse.builder()
                .id(1L)
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method("CARD")
                .status("PAID")
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(customerId, 1L)).thenReturn(true);
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(cardPayment);
        when(modelMapper.map(cardPayment, PaymentResponse.class)).thenReturn(cardResponse);

        // When
        PaymentResponse result = paymentService.processPayment(cardRequest, customerId);

        // Then
        assertNotNull(result);
        assertEquals("CARD", result.getMethod());
    }

    @Test
    void processPayment_WithUpiMethod_ShouldSucceed() {
        // Given
        Long customerId = 1L;
        PaymentRequest upiRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.UPI)
                .build();

        Payment upiPayment = Payment.builder()
                .id(1L)
                .appointment(appointment)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.UPI)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        PaymentResponse upiResponse = PaymentResponse.builder()
                .id(1L)
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method("UPI")
                .status("PAID")
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(reviewRepository.existsByCustomerIdAndAppointmentId(customerId, 1L)).thenReturn(true);
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(upiPayment);
        when(modelMapper.map(upiPayment, PaymentResponse.class)).thenReturn(upiResponse);

        // When
        PaymentResponse result = paymentService.processPayment(upiRequest, customerId);

        // Then
        assertNotNull(result);
        assertEquals("UPI", result.getMethod());
    }

    @Test
    void getPaymentByAppointmentId_ValidId_ShouldReturnPayment() {
        // Given
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.of(payment));
        when(modelMapper.map(payment, PaymentResponse.class)).thenReturn(paymentResponse);

        // When
        PaymentResponse result = paymentService.getPaymentByAppointmentId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getAppointmentId());

        verify(paymentRepository).findByAppointmentId(1L);
        verify(modelMapper).map(payment, PaymentResponse.class);
    }

    @Test
    void getPaymentByAppointmentId_PaymentNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            paymentService.getPaymentByAppointmentId(1L));

        verify(paymentRepository).findByAppointmentId(1L);
        verifyNoInteractions(modelMapper);
    }
}