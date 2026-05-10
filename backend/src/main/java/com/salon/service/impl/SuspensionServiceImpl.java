package com.salon.service.impl;

import com.salon.dto.response.UserStatusResponse;
import com.salon.entity.ComplaintStatus;
import com.salon.entity.UserStatus;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.ComplaintRepository;
import com.salon.repository.CustomerRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.service.SuspensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuspensionServiceImpl implements SuspensionService {

    private final ProfessionalRepository professionalRepository;
    private final CustomerRepository customerRepository;
    private final ComplaintRepository complaintRepository;

    private static final int AUTO_SUSPEND_THRESHOLD = 5;

    @Override
    @Transactional
    public void autoSuspendProfessionalIfNeeded(Long professionalId) {
        long openCount = complaintRepository.countByProfessionalIdAndStatusIn(
                professionalId, List.of(ComplaintStatus.OPEN, ComplaintStatus.FORWARDED));

        if (openCount > AUTO_SUSPEND_THRESHOLD) {
            professionalRepository.findById(professionalId).ifPresent(p -> {
                p.setStatus(UserStatus.SUSPENDED);
                professionalRepository.save(p);
                log.warn("Professional {} auto-suspended due to {} unresolved complaints", professionalId, openCount);
            });
        }
    }

    @Override
    @Transactional
    public void handleAppointmentCancellation(Long customerId, LocalDate appointmentDate) {
        if (!appointmentDate.equals(LocalDate.now())) {
            return; // Only track same-day cancellations
        }

        customerRepository.findById(customerId).ifPresent(customer -> {
            int newCount = customer.getCancelCount() + 1;
            customer.setCancelCount(newCount);

            if (newCount == 1) {
                log.warn("Customer {} has their first same-day cancellation — warning issued", customerId);
            } else if (newCount >= 3) {
                customer.setStatus(UserStatus.SUSPENDED);
                customer.setSuspensionReason("Account suspended due to " + newCount
                        + " same-day appointment cancellations. Your account will be reactivated once reviewed by admin.");
                log.warn("Customer {} suspended after {} same-day cancellations", customerId, newCount);
            }

            customerRepository.save(customer);
        });
    }

    @Override
    @Transactional
    public UserStatusResponse updateUserStatus(Long userId, String userType, String status) {
        return updateUserStatus(userId, userType, status, null);
    }

    @Override
    @Transactional
    public UserStatusResponse updateUserStatus(Long userId, String userType, String status, String reason) {
        UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());

        if ("CUSTOMER".equalsIgnoreCase(userType)) {
            var customer = customerRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + userId));
            customer.setStatus(newStatus);
            if (newStatus == UserStatus.SUSPENDED) {
                customer.setSuspensionReason(reason != null && !reason.isBlank()
                        ? reason
                        : "Your account has been suspended by the admin. Please contact support to resolve this.");
            } else {
                customer.setSuspensionReason(null); // clear on reactivation
            }
            customerRepository.save(customer);
            return new UserStatusResponse(customer.getId(), customer.getName(), "CUSTOMER", newStatus.name());

        } else if ("PROFESSIONAL".equalsIgnoreCase(userType)) {
            var professional = professionalRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Professional not found with id: " + userId));
            professional.setStatus(newStatus);
            professionalRepository.save(professional);
            return new UserStatusResponse(professional.getId(), professional.getName(), "PROFESSIONAL", newStatus.name());

        } else {
            throw new IllegalArgumentException("Invalid userType: must be CUSTOMER or PROFESSIONAL");
        }
    }
}
