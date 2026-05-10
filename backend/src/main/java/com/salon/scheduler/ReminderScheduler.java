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
 * Scheduled job that runs every 30 minutes to:
 *
 * 1. Detect CONFIRMED appointments whose time has passed but payment hasn't been made.
 * 2. Send numbered PAYMENT_REMINDER CustomerNotifications at 8-hour intervals (max 3).
 *    - Reminder 1 (T+8h):  "Hi [Name], you booked [Service] but your payment is still pending..."
 *    - Reminder 2 (T+16h): "...second reminder. Please pay now to avoid account suspension."
 *    - Reminder 3 (T+24h): "...FINAL reminder. If you do not complete payment immediately,
 *                            your account will be suspended."
 * 3. After 3 unanswered reminders:
 *    - Suspend the customer account with a clear reason.
 *    - Send the customer an ACCOUNT_SUSPENDED notification.
 *    - Send the admin a detailed AdminNotification (exactly once per appointment).
 * 4. If payment is completed before the 3rd reminder, the appointment no longer appears
 *    in findOverdueUnpaid() so no further reminders are sent automatically.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final CustomerNotificationRepository customerNotificationRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final CustomerRepository customerRepository;

    /** Interval between reminders in hours */
    private static final int REMINDER_INTERVAL_HOURS = 8;
    /** Maximum number of reminders before suspension */
    private static final int MAX_REMINDERS = 3;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    /** Runs every 30 minutes */
    @Scheduled(fixedRate = 1_800_000)
    @Transactional
    public void processOverdueAppointments() {
        log.info("ReminderScheduler: checking for overdue unpaid appointments");
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> overdueList = appointmentRepository.findOverdueUnpaid(now);
        log.info("ReminderScheduler: found {} overdue unpaid appointments", overdueList.size());

        for (Appointment appt : overdueList) {
            try {
                processAppointment(appt, now);
            } catch (Exception e) {
                log.error("ReminderScheduler: error processing appointment id={}: {}",
                        appt.getId(), e.getMessage(), e);
            }
        }
    }

    private void processAppointment(Appointment appt, LocalDateTime now) {
        if (appt.getReminderCount() == null) appt.setReminderCount(0);

        Customer customer = appt.getCustomer();
        String customerName = customer.getName();
        String serviceName  = appt.getService() != null ? appt.getService().getName() : "your service";
        String dateStr      = appt.getDateTime() != null ? appt.getDateTime().format(FMT) : "N/A";
        String amount       = appt.getService() != null && appt.getService().getPrice() != null
                              ? "₹" + appt.getService().getPrice() : "the service amount";

        int count = appt.getReminderCount();

        // ── Step 1: Send up to 3 reminders at 8-hour intervals ──────────────
        if (count < MAX_REMINDERS) {
            // Check if enough time has passed since the last reminder (or since appointment time)
            LocalDateTime lastSent = appt.getLastReminderSentAt() != null
                    ? appt.getLastReminderSentAt()
                    : appt.getDateTime();

            if (lastSent != null && lastSent.isAfter(now.minusHours(REMINDER_INTERVAL_HOURS))) {
                log.debug("ReminderScheduler: skipping appt id={} — next reminder not due yet", appt.getId());
                return;
            }

            int nextCount = count + 1;
            String message = buildReminderMessage(nextCount, customerName, serviceName, dateStr);

            customerNotificationRepository.save(CustomerNotification.builder()
                    .customer(customer)
                    .type(CustomerNotificationType.PAYMENT_REMINDER)
                    .referenceId(appt.getId())
                    .message(message)
                    .build());

            appt.setReminderCount(nextCount);
            appt.setLastReminderSentAt(now);
            appointmentRepository.save(appt);

            log.info("ReminderScheduler: sent PAYMENT_REMINDER #{} to customer id={} (appt id={})",
                    nextCount, customer.getId(), appt.getId());

        // ── Step 2: After 3 reminders — suspend + notify admin ───────────────
        } else {
            // Only act once — check if admin has already been notified for this appointment
            if (adminNotificationRepository.existsByReferenceId(appt.getId())) {
                log.debug("ReminderScheduler: already escalated appt id={}", appt.getId());
                return;
            }

            // 2a. Suspend the customer
            if (customer.getStatus() != UserStatus.SUSPENDED) {
                String suspensionReason = "Your account has been suspended due to non-payment of "
                        + amount + " for " + serviceName + " booked on " + dateStr
                        + ". Please complete your payment to have your account reactivated.";
                customer.setStatus(UserStatus.SUSPENDED);
                customer.setSuspensionReason(suspensionReason);
                customerRepository.save(customer);
                log.warn("ReminderScheduler: suspended customer id={} for non-payment (appt id={})",
                        customer.getId(), appt.getId());

                // 2b. Notify the customer about suspension
                String suspendMsg = "Hi " + customerName + ", your account has been suspended "
                        + "due to non-payment of " + amount + " for " + serviceName
                        + " booked on " + dateStr + ". "
                        + "Please complete your payment immediately to restore your account. "
                        + "Contact support if you need assistance.";
                customerNotificationRepository.save(CustomerNotification.builder()
                        .customer(customer)
                        .type(CustomerNotificationType.ACCOUNT_SUSPENDED)
                        .referenceId(appt.getId())
                        .message(suspendMsg)
                        .build());
            }

            // 2c. Notify admin with full details
            String r1 = appt.getDateTime() != null
                    ? appt.getDateTime().plusHours(REMINDER_INTERVAL_HOURS).format(FMT) : "N/A";
            String r2 = appt.getDateTime() != null
                    ? appt.getDateTime().plusHours(REMINDER_INTERVAL_HOURS * 2L).format(FMT) : "N/A";
            String r3 = appt.getDateTime() != null
                    ? appt.getDateTime().plusHours(REMINDER_INTERVAL_HOURS * 3L).format(FMT) : "N/A";

            String adminMessage = "⚠️ ACTION REQUIRED: Customer " + customerName
                    + " (ID: " + customer.getId() + ")"
                    + " | Email: " + (customer.getEmail() != null ? customer.getEmail() : "N/A")
                    + " | Phone: " + (customer.getPhone() != null ? customer.getPhone() : "N/A")
                    + " has NOT completed payment of " + amount
                    + " for service: " + serviceName
                    + " | Booked: " + dateStr
                    + " | Reminders sent: Reminder 1 @ " + r1
                    + ", Reminder 2 @ " + r2
                    + ", Reminder 3 @ " + r3
                    + " | Account has been automatically suspended."
                    + " Please review and take further action.";

            adminNotificationRepository.save(AdminNotification.builder()
                    .message(adminMessage)
                    .referenceId(appt.getId())
                    .build());

            log.warn("ReminderScheduler: admin notified and customer id={} suspended for appt id={}",
                    customer.getId(), appt.getId());
        }
    }

    private String buildReminderMessage(int reminderNumber, String name,
                                        String service, String date) {
        return switch (reminderNumber) {
            case 1 -> "Hi " + name + ", you booked " + service + " on " + date
                    + " but your payment is still pending. "
                    + "Please complete your payment to confirm your booking.";
            case 2 -> "Hi " + name + ", your payment for " + service + " on " + date
                    + " is still incomplete. This is your second reminder. "
                    + "Please pay now to avoid account suspension.";
            case 3 -> "Hi " + name + ", this is your FINAL reminder. "
                    + "Your payment for " + service + " on " + date + " is overdue. "
                    + "If you do not complete payment immediately, your account will be suspended.";
            default -> "Hi " + name + ", your payment for " + service + " is still pending. "
                    + "Please complete it as soon as possible.";
        };
    }
}
