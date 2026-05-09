package com.salon.service;

import com.salon.dto.request.PaymentRequest;
import com.salon.dto.response.PaymentResponse;
import com.salon.entity.Appointment;
import com.salon.entity.Payment;
import com.salon.entity.PaymentStatus;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.ValidationException;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.PaymentRepository;
import com.salon.repository.ReviewRepository;
import com.salon.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;
    private final LoyaltyService loyaltyService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, Long customerId) {
        log.info("Processing payment for appointment ID: {} by customer: {}",
                request.getAppointmentId(), customerId);

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // 1. Timing check — payment only allowed at or after the appointment time
        if (appointment.getDateTime().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Payment is not allowed before the appointment time");
        }

        // 2. Duplicate payment guard
        Optional<Payment> existingPayment = paymentRepository.findByAppointmentId(request.getAppointmentId());
        if (existingPayment.isPresent()) {
            throw new ValidationException("Payment already exists for this appointment");
        }

        // 2. Validate payment amount equals service price
        if (appointment.getService() != null && appointment.getService().getPrice() != null) {
            if (appointment.getService().getPrice().compareTo(request.getAmount()) != 0) {
                throw new ValidationException("Payment amount must equal the service price of ₹"
                        + appointment.getService().getPrice());
            }
        }

        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(request.getAmount())
                .method(request.getMethod())
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment processed successfully with ID: {}", savedPayment.getId());

        // Award loyalty points based on service cost
        try {
            loyaltyService.awardPointsOnAppointmentCompletion(request.getAppointmentId());
        } catch (Exception e) {
            log.warn("Failed to award loyalty points for appointment {}: {}", request.getAppointmentId(), e.getMessage());
        }

        return toResponse(savedPayment);
    }

    public PaymentResponse getPaymentByAppointmentId(Long appointmentId) {
        log.info("Retrieving payment for appointment ID: {}", appointmentId);

        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for appointment"));

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .appointmentId(payment.getAppointment().getId())
                .amount(payment.getAmount())
                .method(payment.getMethod() != null ? payment.getMethod().name() : null)
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .paidAt(payment.getPaidAt())
                .build();
    }
}
