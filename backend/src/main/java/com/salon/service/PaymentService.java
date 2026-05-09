package com.salon.service;

import com.salon.dto.request.PaymentRequest;
import com.salon.dto.response.PaymentResponse;
import com.salon.entity.Appointment;
import com.salon.entity.Payment;
import com.salon.entity.PaymentMethod;
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

import java.math.BigDecimal;
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
    private final WalletService walletService;

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

        // 3. Validate total (walletAmountUsed + amount) equals service price
        BigDecimal walletUsed = request.getWalletAmountUsed() != null
                ? request.getWalletAmountUsed()
                : BigDecimal.ZERO;
        BigDecimal totalPaid = request.getAmount().add(walletUsed);

        if (appointment.getService() != null && appointment.getService().getPrice() != null) {
            BigDecimal servicePrice = appointment.getService().getPrice();
            if (servicePrice.compareTo(totalPaid) != 0) {
                throw new ValidationException("Total payment (wallet + method) must equal the service price of ₹"
                        + servicePrice);
            }
        }

        // 4. Deduct wallet balance if wallet was used
        if (walletUsed.compareTo(BigDecimal.ZERO) > 0) {
            walletService.debit(
                    appointment.getCustomer(),
                    walletUsed,
                    "appointment_payment",
                    "Wallet payment for appointment #" + appointment.getId()
                            + " (" + appointment.getService().getName() + ")"
            );
            log.info("Deducted ₹{} from wallet for appointment {}", walletUsed, appointment.getId());
        }

        // 5. Determine the stored payment method:
        //    - If fully paid by wallet, store WALLET
        //    - Otherwise store the chosen cash/card method
        PaymentMethod storedMethod = (request.getAmount().compareTo(BigDecimal.ZERO) == 0)
                ? PaymentMethod.WALLET
                : request.getMethod();

        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(totalPaid)          // store full service price
                .method(storedMethod)
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
