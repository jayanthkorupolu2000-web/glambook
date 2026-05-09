package com.salon.scheduler;

import com.salon.entity.*;
import com.salon.repository.AdminNotificationRepository;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.CustomerNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduled job that runs every 60 minutes to:
 * 1. Detect CONFIRMED appointments whose time has passed but payment hasn't been made.
 * 2. Send PAYMENT_REMINDER CustomerNotifications (up to 3 times, max once per 24 h).
 * 3. After 3 unanswered reminders, escalate to Admin via AdminNotification (exactly once).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final CustomerNotificationRepository customerNotificationRepository;
    private final AdminNotificationRepository adminNotificationRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Scheduled(fixedRate = 3_600_000) // every 60 minutes
    @Transactional
    public void processOverdueAppointments() {
        log.info("ReminderScheduler: checking for overdue unpaid appointments");
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> overdueList = appointmentRepository.findOverdueUnpaid(now);
        log.info("ReminderScheduler: found {} overdue appointments", overdueList.size());

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
        String serviceName = appt.getService() != null ? appt.getService().getName() : "Service";
        String profName = appt.getProfessional() != null ? appt.getProfessional().getName() : "Professional";
        String dateStr = appt.getDateTime() != null ? appt.getDateTime().format(FORMATTER) : "N/A";
        Customer customer = appt.getCustomer();

        if (appt.getReminderCount() == null) appt.setReminderCount(0);

        if (appt.getReminderCount() < 3) {
            // Only send if no reminder in the last 24 hours
            boolean withinWindow = appt.getLastReminderSentAt() != null
                    && appt.getLastReminderSentAt().isAfter(now.minusHours(24));
            if (withinWindow) {
                log.debug("ReminderScheduler: skipping appt id={} — reminder sent within 24h", appt.getId());
                return;
            }

            String message = "Reminder: Your appointment for " + serviceName
                    + " with " + profName + " on " + dateStr
                    + " is unpaid. Please complete your payment to avoid further action.";

            CustomerNotification notification = CustomerNotification.builder()
                    .customer(customer)
                    .type(CustomerNotificationType.PAYMENT_REMINDER)
                    .referenceId(appt.getId())
                    .message(message)
                    .build();
            customerNotificationRepository.save(notification);

            appt.setReminderCount(appt.getReminderCount() + 1);
            appt.setLastReminderSentAt(now);
            appointmentRepository.save(appt);

            log.info("ReminderScheduler: sent PAYMENT_REMINDER #{} to customer id={} for appt id={}",
                    appt.getReminderCount(), customer.getId(), appt.getId());

        } else {
            // 3 reminders sent — escalate to admin if not already done
            if (adminNotificationRepository.existsByReferenceId(appt.getId())) {
                log.debug("ReminderScheduler: admin already notified for appt id={}", appt.getId());
                return;
            }

            String adminMessage = "Customer " + customer.getName()
                    + " (ID: " + customer.getId() + ") has not paid after 3 reminders for "
                    + serviceName + " with " + profName + " on " + dateStr
                    + ". Please review and take action.";

            AdminNotification adminNotification = AdminNotification.builder()
                    .message(adminMessage)
                    .referenceId(appt.getId())
                    .build();
            adminNotificationRepository.save(adminNotification);

            log.warn("ReminderScheduler: escalated appt id={} to admin — customer id={} has not paid",
                    appt.getId(), customer.getId());
        }
    }
}
