package com.salon.service;

import com.salon.dto.response.LoyaltyResponse;
import com.salon.entity.*;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles the Pay Later flow:
 *  - Eligibility check (Silver+ tier, points value >= serviceCost/2, not suspended)
 *  - Opt-in: creates a PAY_LATER_PENDING payment record with a 24-hour deadline
 *  - Settlement: converts PAY_LATER_PENDING → PAID when customer pays
 *  - Scheduled reminders and auto-suspension are handled by PayLaterScheduler
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayLaterService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerNotificationRepository customerNotificationRepository;
    private final WalletService walletService;
    private final LoyaltyService loyaltyService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── Eligibility ───────────────────────────────────────────────────────────

    /**
     * Rule 1: Tier must be SILVER, GOLD, or PLATINUM (based on totalEarned).
     * Rule 2: (availablePoints / 10) >= (serviceCost / 2)
     *         i.e. wallet value of points >= half the service cost.
     * Rule 3: Account must not be suspended.
     */
    public Map<String, Object> checkEligibility(Long customerId, Long appointmentId) {
        Map<String, Object> result = new LinkedHashMap<>();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Rule 3: not suspended
        if (customer.getStatus() == UserStatus.SUSPENDED) {
            result.put("eligible", false);
            result.put("reason", "Your account is suspended. Pay Later is not available.");
            return result;
        }

        // Get loyalty summary
        LoyaltyResponse summary = loyaltyService.getSummary(customerId);
        String tier = summary.getTier();
        int availablePoints = summary.getPoints();

        // Rule 1: Silver or above
        boolean tierOk = "SILVER".equals(tier) || "GOLD".equals(tier) || "DIAMOND".equals(tier);
        if (!tierOk) {
            result.put("eligible", false);
            result.put("reason", "Pay Later requires Silver tier or above. Your current tier is " + tier + ".");
            result.put("tier", tier);
            result.put("availablePoints", availablePoints);
            return result;
        }

        // Rule 2: points value (pts / 10 = ₹) >= half service cost
        BigDecimal serviceCost = appt.getService() != null && appt.getService().getPrice() != null
                ? appt.getService().getPrice() : BigDecimal.ZERO;
        BigDecimal pointsValue = BigDecimal.valueOf(availablePoints)
                .divide(BigDecimal.TEN, 2, java.math.RoundingMode.DOWN);
        BigDecimal halfCost = serviceCost.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);

        if (pointsValue.compareTo(halfCost) < 0) {
            result.put("eligible", false);
            result.put("reason", "Insufficient loyalty points. You need points worth at least ₹"
                    + halfCost + " (half the service cost of ₹" + serviceCost + "). "
                    + "Your " + availablePoints + " points are worth ₹" + pointsValue + ".");
            result.put("tier", tier);
            result.put("availablePoints", availablePoints);
            result.put("pointsValue", pointsValue);
            result.put("requiredValue", halfCost);
            return result;
        }

        result.put("eligible", true);
        result.put("tier", tier);
        result.put("availablePoints", availablePoints);
        result.put("pointsValue", pointsValue);
        result.put("serviceCost", serviceCost);
        result.put("deadline", LocalDateTime.now().plusHours(24).format(FMT));
        return result;
    }

    // ── Opt-in ────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> optIn(Long customerId, Long appointmentId) {
        Map<String, Object> eligibility = checkEligibility(customerId, appointmentId);
        if (!(Boolean) eligibility.get("eligible")) {
            throw new InvalidOperationException((String) eligibility.get("reason"));
        }

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Duplicate guard — if already opted in, return existing record
        java.util.Optional<Payment> existing = paymentRepository.findByAppointmentId(appointmentId);
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (p.getStatus() == PaymentStatus.PAY_LATER_PENDING) {
                // Already opted in — return existing deadline
                Map<String, Object> already = new java.util.LinkedHashMap<>();
                already.put("paymentId", p.getId());
                already.put("appointmentId", appointmentId);
                already.put("amount", p.getAmount());
                already.put("deadline", p.getPayLaterDeadline() != null ? p.getPayLaterDeadline().format(FMT) : "N/A");
                already.put("status", "PAY_LATER_PENDING");
                return already;
            }
            throw new InvalidOperationException("A payment record already exists for this appointment");
        }

        BigDecimal amount = appt.getService() != null && appt.getService().getPrice() != null
                ? appt.getService().getPrice() : BigDecimal.ZERO;
        LocalDateTime deadline = LocalDateTime.now().plusHours(24);

        Payment payment = Payment.builder()
                .appointment(appt)
                .amount(amount)
                .method(PaymentMethod.PAY_LATER)
                .status(PaymentStatus.PAY_LATER_PENDING)
                .payLaterDeadline(deadline)
                .payLaterReminderCount(0)
                .build();
        Payment saved = paymentRepository.save(payment);

        // Confirmation notification
        String serviceName = appt.getService() != null ? appt.getService().getName() : "your service";
        String msg = "✅ Pay Later activated for " + serviceName + ". "
                + "Amount due: ₹" + amount + ". "
                + "Deadline: " + deadline.format(FMT) + ". "
                + "Please complete payment before the deadline to avoid account suspension.";
        customerNotificationRepository.save(CustomerNotification.builder()
                .customer(appt.getCustomer())
                .type(CustomerNotificationType.PAY_LATER_CONFIRMED)
                .referenceId(appt.getId())
                .message(msg)
                .build());

        log.info("Pay Later opted in: customer={}, appointment={}, deadline={}", customerId, appointmentId, deadline);

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("paymentId", saved.getId());
        response.put("appointmentId", appointmentId);
        response.put("amount", amount);
        response.put("deadline", deadline.format(FMT));
        response.put("status", "PAY_LATER_PENDING");
        return response;
    }

    // ── Settlement ────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> settle(Long customerId, Long appointmentId,
                                      String method, BigDecimal walletAmountUsed) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("No Pay Later record found for this appointment"));

        if (payment.getStatus() != PaymentStatus.PAY_LATER_PENDING) {
            throw new InvalidOperationException("This appointment does not have a pending Pay Later payment");
        }

        Appointment appt = payment.getAppointment();
        if (!appt.getCustomer().getId().equals(customerId)) {
            throw new InvalidOperationException("This payment does not belong to you");
        }

        BigDecimal walletUsed = walletAmountUsed != null ? walletAmountUsed : BigDecimal.ZERO;

        // Deduct wallet if used
        if (walletUsed.compareTo(BigDecimal.ZERO) > 0) {
            walletService.debit(appt.getCustomer(), walletUsed,
                    "pay_later_settlement",
                    "Wallet used to settle Pay Later for appointment #" + appointmentId);
        }

        // Mark as PAID
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        PaymentMethod storedMethod = walletUsed.compareTo(payment.getAmount()) >= 0
                ? PaymentMethod.WALLET
                : PaymentMethod.valueOf(method != null ? method.toUpperCase() : "CASH");
        payment.setMethod(storedMethod);
        paymentRepository.save(payment);

        // Award loyalty points
        try {
            loyaltyService.awardPointsOnAppointmentCompletion(appointmentId);
        } catch (Exception e) {
            log.warn("Failed to award loyalty points for Pay Later settlement: {}", e.getMessage());
        }

        // Reactivate account if suspended for this non-payment
        Customer customer = appt.getCustomer();
        if (customer.getStatus() == UserStatus.SUSPENDED
                && customer.getSuspensionReason() != null
                && customer.getSuspensionReason().contains("Pay Later")) {
            customer.setStatus(UserStatus.ACTIVE);
            customer.setSuspensionReason(null);
            customerRepository.save(customer);
            log.info("Customer {} reactivated after Pay Later settlement for appointment {}", customerId, appointmentId);
        }

        log.info("Pay Later settled: customer={}, appointment={}, method={}", customerId, appointmentId, storedMethod);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "PAID");
        response.put("paidAt", payment.getPaidAt().format(FMT));
        response.put("amount", payment.getAmount());
        return response;
    }
}
