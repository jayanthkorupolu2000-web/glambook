package com.salon.scheduler;

import com.salon.entity.*;
import com.salon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduled job for Pay Later reminders and auto-suspension.
 *
 * Timeline (relative to opt-in time T):
 *   T+8h  → Reminder 1: "16 hours left to pay ₹{amount}"
 *   T+16h → Reminder 2: "Only 8 hours remain. Non-payment will zero your points and suspend your account."
 *   T+24h → If still unpaid: zero loyalty points + suspend account + admin alert
 *
 * Runs every 30 minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayLaterScheduler {

    private final PaymentRepository paymentRepository;
    private final CustomerNotificationRepository customerNotificationRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyRepository loyaltyRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Scheduled(fixedRate = 1_800_000) // every 30 minutes
    @Transactional
    public void processPayLaterReminders() {
        log.info("PayLaterScheduler: checking pending Pay Later payments");
        LocalDateTime now = LocalDateTime.now();
        List<Payment> pending = paymentRepository.findAllPayLaterPending();
        log.info("PayLaterScheduler: found {} pending Pay Later payments", pending.size());

        for (Payment payment : pending) {
            try {
                processPayment(payment, now);
            } catch (Exception e) {
                log.error("PayLaterScheduler: error processing payment id={}: {}",
                        payment.getId(), e.getMessage(), e);
            }
        }
    }

    private void processPayment(Payment payment, LocalDateTime now) {
        LocalDateTime deadline = payment.getPayLaterDeadline();
        if (deadline == null) return;

        // Opt-in time = deadline - 24h
        LocalDateTime optInTime = deadline.minusHours(24);
        int reminderCount = payment.getPayLaterReminderCount() != null ? payment.getPayLaterReminderCount() : 0;

        Appointment appt = payment.getAppointment();
        Customer customer = appt.getCustomer();
        String serviceName = appt.getService() != null ? appt.getService().getName() : "your service";
        String amountStr = "₹" + payment.getAmount();

        // ── T+24h: deadline passed — auto-suspend ────────────────────────────
        if (now.isAfter(deadline)) {
            // Only act once — check if admin already notified for this payment
            if (adminNotificationRepository.existsByReferenceId(payment.getId())) {
                log.debug("PayLaterScheduler: already processed deadline for payment id={}", payment.getId());
                return;
            }

            // 1. Zero out loyalty points
            zeroLoyaltyPoints(customer);

            // 2. Suspend account
            String suspensionReason = "Your account has been suspended due to non-payment via Pay Later. "
                    + "Amount of " + amountStr + " for " + serviceName + " was due by "
                    + deadline.format(FMT) + ". "
                    + "Your loyalty points have been reset to 0. "
                    + "Please contact support to resolve this and reactivate your account.";
            customer.setStatus(UserStatus.SUSPENDED);
            customer.setSuspensionReason(suspensionReason);
            customerRepository.save(customer);

            // 3. Notify customer
            customerNotificationRepository.save(CustomerNotification.builder()
                    .customer(customer)
                    .type(CustomerNotificationType.ACCOUNT_SUSPENDED)
                    .referenceId(appt.getId())
                    .message("🚫 Your account has been suspended due to non-payment of "
                            + amountStr + " for " + serviceName + ". "
                            + "Your loyalty points have been reset to 0. "
                            + "Please contact support to resolve this.")
                    .build());

            // 4. Notify admin
            String adminMsg = "⚠️ PAY LATER EXPIRED: Customer " + customer.getName()
                    + " (ID: " + customer.getId() + ")"
                    + " | Email: " + (customer.getEmail() != null ? customer.getEmail() : "N/A")
                    + " | Phone: " + (customer.getPhone() != null ? customer.getPhone() : "N/A")
                    + " did NOT settle Pay Later of " + amountStr + " for " + serviceName
                    + " | Opt-in: " + optInTime.format(FMT)
                    + " | Deadline: " + deadline.format(FMT)
                    + " | Reminders sent: " + reminderCount
                    + " | Account suspended + loyalty points zeroed.";
            adminNotificationRepository.save(AdminNotification.builder()
                    .message(adminMsg)
                    .referenceId(payment.getId())
                    .build());

            log.warn("PayLaterScheduler: deadline expired — suspended customer id={}, zeroed points, payment id={}",
                    customer.getId(), payment.getId());
            return;
        }

        // ── T+8h: first reminder ─────────────────────────────────────────────
        LocalDateTime reminder1Due = optInTime.plusHours(8);
        if (reminderCount == 0 && now.isAfter(reminder1Due)) {
            long hoursLeft = java.time.Duration.between(now, deadline).toHours();
            String msg = "⏰ Pay Later Reminder: " + hoursLeft + " hours left to pay "
                    + amountStr + " for " + serviceName + ". "
                    + "Deadline: " + deadline.format(FMT) + ".";
            sendReminder(customer, appt.getId(), msg);
            payment.setPayLaterReminderCount(1);
            paymentRepository.save(payment);
            log.info("PayLaterScheduler: sent reminder 1 to customer id={} for payment id={}",
                    customer.getId(), payment.getId());
            return;
        }

        // ── T+16h: second reminder ───────────────────────────────────────────
        LocalDateTime reminder2Due = optInTime.plusHours(16);
        if (reminderCount == 1 && now.isAfter(reminder2Due)) {
            String msg = "🚨 URGENT — Pay Later: Only 8 hours remain to pay "
                    + amountStr + " for " + serviceName + ". "
                    + "Deadline: " + deadline.format(FMT) + ". "
                    + "Non-payment will zero your loyalty points and suspend your account.";
            sendReminder(customer, appt.getId(), msg);
            payment.setPayLaterReminderCount(2);
            paymentRepository.save(payment);
            log.info("PayLaterScheduler: sent reminder 2 to customer id={} for payment id={}",
                    customer.getId(), payment.getId());
        }
    }

    private void sendReminder(Customer customer, Long referenceId, String message) {
        customerNotificationRepository.save(CustomerNotification.builder()
                .customer(customer)
                .type(CustomerNotificationType.PAYMENT_REMINDER)
                .referenceId(referenceId)
                .message(message)
                .build());
    }

    /** Zero out all loyalty points for the customer (penalty for non-payment). */
    private void zeroLoyaltyPoints(Customer customer) {
        List<Loyalty> records = loyaltyRepository.findByCustomerId(customer.getId());
        records.forEach(l -> {
            l.setPoints(0);
            l.setUpdatedAt(java.time.LocalDateTime.now());
            loyaltyRepository.save(l);
        });
        // Record a REDEEM transaction to zero out the balance in transaction history
        int totalEarned = loyaltyTransactionRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().filter(t -> "EARN".equals(t.getType())).mapToInt(LoyaltyTransaction::getPoints).sum();
        int totalRedeemed = loyaltyTransactionRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().filter(t -> "REDEEM".equals(t.getType())).mapToInt(LoyaltyTransaction::getPoints).sum();
        int available = totalEarned - totalRedeemed;
        if (available > 0) {
            loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                    .customer(customer)
                    .type("REDEEM")
                    .points(available)
                    .description("Loyalty points zeroed due to Pay Later non-payment")
                    .build());
        }
        log.info("PayLaterScheduler: zeroed {} loyalty points for customer id={}", available, customer.getId());
    }
}
