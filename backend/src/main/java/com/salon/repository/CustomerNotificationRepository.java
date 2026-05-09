package com.salon.repository;

import com.salon.entity.CustomerNotification;
import com.salon.entity.CustomerNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {
    List<CustomerNotification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<CustomerNotification> findByCustomerIdAndIsRead(Long customerId, boolean isRead);
    long countByCustomerIdAndIsRead(Long customerId, boolean isRead);
    long countByCustomerIdAndReferenceIdAndType(Long customerId, Long referenceId, CustomerNotificationType type);
}
