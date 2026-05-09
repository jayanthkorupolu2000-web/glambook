package com.salon.service.impl;

import com.salon.dto.response.CustomerNotificationResponse;
import com.salon.entity.Customer;
import com.salon.entity.CustomerNotification;
import com.salon.entity.CustomerNotificationType;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.CustomerNotificationRepository;
import com.salon.repository.CustomerRepository;
import com.salon.service.CustomerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerNotificationServiceImpl implements CustomerNotificationService {

    private final CustomerNotificationRepository notifRepo;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void createNotification(Long customerId, CustomerNotificationType type, Long referenceId, String message) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        CustomerNotification notif = CustomerNotification.builder()
                .customer(customer).type(type).referenceId(referenceId).message(message).build();
        notifRepo.save(notif);
    }

    @Override
    public List<CustomerNotificationResponse> getNotifications(Long customerId) {
        return notifRepo.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long customerId) {
        return notifRepo.countByCustomerIdAndIsRead(customerId, false);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long customerId) {
        notifRepo.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notifRepo.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long customerId) {
        List<CustomerNotification> unread = notifRepo.findByCustomerIdAndIsRead(customerId, false);
        unread.forEach(n -> n.setRead(true));
        notifRepo.saveAll(unread);
    }

    private CustomerNotificationResponse toResponse(CustomerNotification n) {
        CustomerNotificationResponse res = new CustomerNotificationResponse();
        res.setId(n.getId());
        res.setType(n.getType().name());
        res.setReferenceId(n.getReferenceId());
        res.setMessage(n.getMessage());
        res.setRead(n.isRead());
        res.setCreatedAt(n.getCreatedAt());
        return res;
    }
}
