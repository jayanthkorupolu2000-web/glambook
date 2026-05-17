package com.salon.payment;

import com.salon.dto.request.PaymentRequest;
import com.salon.dto.response.PaymentResponse;
import com.salon.entity.*;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.ValidationException;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.PaymentRepository;
import com.salon.repository.ReviewRepository;
import com.salon.service.LoyaltyService;
import com.salon.service.PaymentService;
import com.salon.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private LoyaltyService loyaltyService;
    @Mock private WalletService walletService;

    @InjectMocks private PaymentService paymentService;

    private Customer customer;
    private Service service;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice").email("alice@gmail.com").build();

        service = Service.builder()
                .id(1L).name("Haircut").price(new BigDecimal("300.00")).durationMins(30).build();

        appointment = Appointment.builder()
                .id(1L).customer(customer).service(service)
                .dateTime(LocalDateTime.now().minusHours(1))   // past — payment allowed
                .status(AppointmentStatus.CONFIRMED)
                .build();
    }

    // ── processPayment ────────────────────────────────────────────────────────

    @Test
    void processPayment_CashFullAmount_ShouldSaveAndReturnResponse() {
        PaymentRequest req = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("300.00"))
                .method(PaymentMethod.CASH)
                .build();

        Payment saved = Payment.builder()
                .id(10L).appointment(appointment)
                .amount(new BigDecimal("300.00"))
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        doNothing().when(loyaltyService).awardPointsOnAppointmentCompletion(1L);

        PaymentResponse response = paymentService.processPayment(req, 1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("PAID", response.getStatus());
        assertEquals("CASH", response.getMethod());
        assertEquals(new BigDecimal("300.00"), response.getAmount());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPayment_WithWallet_ShouldDebitWalletAndSave() {
        PaymentRequest req = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("100.00"))
                .walletAmountUsed(new BigDecimal("200.00"))
                .method(PaymentMethod.CASH)
                .build();

        Payment saved = Payment.builder()
                .id(11L).appointment(appointment)
                .amount(new BigDecimal("300.00"))
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        doNothing().when(loyaltyService).awardPointsOnAppointmentCompletion(1L);

        paymentService.processPayment(req, 1L);

        verify(walletService).debit(eq(customer), eq(new BigDecimal("200.00")), anyString(), anyString());
    }

    @Test
    void processPayment_FullyByWallet_ShouldStoreWalletMethod() {
        PaymentRequest req = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(BigDecimal.ZERO)
                .walletAmountUsed(new BigDecimal("300.00"))
                .method(PaymentMethod.CASH)
                .build();

        Payment saved = Payment.builder()
                .id(12L).appointment(appointment)
                .amount(new BigDecimal("300.00"))
                .method(PaymentMethod.WALLET)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        doNothing().when(loyaltyService).awardPointsOnAppointmentCompletion(1L);

        PaymentResponse response = paymentService.processPayment(req, 1L);

        assertEquals("WALLET", response.getMethod());
    }

    @Test
    void processPayment_AppointmentNotFound_ShouldThrow() {
        PaymentRequest req = PaymentRequest.builder()
                .appointmentId(99L).amount(new BigDecimal("300.00")).build();

        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.processPayment(req, 1L));
    }

    @Test
    void processPayment_BeforeAppointmentTime_ShouldThrowValidationException() {
        appointment.setDateTime(LocalDateTime.now().plusHours(2)); // future
        PaymentRequest req = PaymentRequest.builder()
                .appointmentId(1L).amount(new BigDecimal("300.00")).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(ValidationException.class,
                () -> paymentService.processPayment(req, 1L));
    }

    @Test
    void processPayment_DuplicatePayment_ShouldThrowValidationException() {
        PaymentRequest req = PaymentRequest.builder()
                .appointmentId(1L).amount(new BigDecimal("300.00")).build();

        Payment existing = Payment.builder().id(5L).build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.of(existing));

        assertThrows(ValidationException.class,
                () -> paymentService.processPayment(req, 1L));
    }

    @Test
    void processPayment_WrongTotalAmount_ShouldThrowValidationException() {
        PaymentRequest req = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("200.00"))  // service price is 300
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> paymentService.processPayment(req, 1L));
    }

    // ── getPaymentByAppointmentId ─────────────────────────────────────────────

    @Test
    void getPaymentByAppointmentId_Exists_ShouldReturnResponse() {
        Payment payment = Payment.builder()
                .id(10L).appointment(appointment)
                .amount(new BigDecimal("300.00"))
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByAppointmentId(1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("PAID", response.getStatus());
    }

    @Test
    void getPaymentByAppointmentId_NotFound_ShouldThrow() {
        when(paymentRepository.findByAppointmentId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentByAppointmentId(99L));
    }
}
