package com.salon.service.impl;

import com.salon.dto.response.*;
import com.salon.entity.*;
import com.salon.repository.*;
import com.salon.service.CustomerDashboardService;
import com.salon.service.CustomerNotificationService;
import com.salon.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerDashboardServiceImpl implements CustomerDashboardService {

    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final LoyaltyRepository loyaltyRepository;
    private final ReviewRepository reviewRepository;
    private final BeautyProfileRepository beautyProfileRepository;
    private final CustomerNotificationRepository notifRepository;
    private final OrderRepository orderRepository;
    private final PolicyService policyService;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CustomerDashboardResponse getDashboard(Long customerId) {
        CustomerDashboardResponse dashboard = new CustomerDashboardResponse();
        // Initialize lists to prevent NPE in frontend
        dashboard.setUpcomingAppointments(Collections.emptyList());
        dashboard.setPendingAppointments(Collections.emptyList());
        dashboard.setPendingReviews(Collections.emptyList());

        try {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) return dashboard;

            dashboard.setCustomerName(customer.getName());
            dashboard.setProfilePhotoUrl(customer.getProfilePhotoUrl());
            dashboard.setCityName(customer.getCity());

            // Upcoming appointments — CONFIRMED or PENDING (not yet completed/cancelled)
            List<Appointment> upcoming = appointmentRepository
                    .findByCustomerId(customerId)
                    .stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED
                              || a.getStatus() == AppointmentStatus.PENDING)
                    .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                    .limit(5)
                    .collect(Collectors.toList());
            dashboard.setUpcomingAppointments(upcoming.stream().map(this::toAppointmentResponse).collect(Collectors.toList()));

            // Pending appointments (awaiting assignment — subset of upcoming)
            List<Appointment> pending = upcoming.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.PENDING)
                    .collect(Collectors.toList());
            dashboard.setPendingAppointments(pending.stream().map(this::toAppointmentResponse).collect(Collectors.toList()));

            // Pending reviews (COMPLETED with no review)
            List<Appointment> completed = appointmentRepository
                    .findByCustomerIdAndStatus(customerId, AppointmentStatus.COMPLETED);
            List<AppointmentResponse> pendingReviews = completed.stream()
                    .filter(a -> !reviewRepository.existsByCustomerIdAndAppointmentId(customerId, a.getId()))
                    .limit(5)
                    .map(this::toAppointmentResponse)
                    .collect(Collectors.toList());
            dashboard.setPendingReviews(pendingReviews);

            // Loyalty points total
            int totalPoints = loyaltyRepository.findByCustomerId(customerId)
                    .stream().mapToInt(Loyalty::getPoints).sum();
            dashboard.setTotalLoyaltyPoints(totalPoints);

            // Unread notifications
            dashboard.setUnreadNotificationCount((int) notifRepository.countByCustomerIdAndIsRead(customerId, false));

            // Pending orders
            dashboard.setPendingOrderCount((int) orderRepository
                    .findByCustomerIdAndDeliveryStatus(customerId, OrderDeliveryStatus.PROCESSING).size());

            // Beauty profile complete
            boolean bpComplete = beautyProfileRepository.findByCustomerId(customerId)
                    .map(bp -> bp.getSkinType() != null && bp.getHairType() != null)
                    .orElse(false);
            dashboard.setBeautyProfileComplete(bpComplete);

            // Latest global policy
            try {
                dashboard.setLatestGlobalPolicy(policyService.getLatestPolicy());
            } catch (Exception e) {
                log.debug("No global policy found");
            }

        } catch (Exception e) {
            log.error("Dashboard error for customer {}: {}", customerId, e.getMessage(), e);
        }

        return dashboard;
    }

    private AppointmentResponse toAppointmentResponse(Appointment a) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(a.getId());
        res.setCustomerId(a.getCustomer().getId());
        res.setCustomerName(a.getCustomer().getName());
        res.setServiceId(a.getService().getId());
        res.setServiceName(a.getService().getName());
        res.setScheduledAt(a.getDateTime());
        res.setStatus(a.getStatus().name());
        if (a.getProfessional() != null) {
            res.setProfessionalId(a.getProfessional().getId());
            res.setProfessionalName(a.getProfessional().getName());
            res.setProfessionalPhotoUrl(a.getProfessional().getProfilePhotoUrl());
        }
        res.setCanCancel(
            (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
            && a.getDateTime().isAfter(LocalDateTime.now())
        );
        res.setCanRebook(a.getStatus() == AppointmentStatus.COMPLETED || a.getStatus() == AppointmentStatus.CANCELLED);
        res.setCanReview(
            a.getStatus() == AppointmentStatus.COMPLETED
            && !reviewRepository.existsByCustomerIdAndAppointmentId(a.getCustomer().getId(), a.getId())
        );
        return res;
    }
}
