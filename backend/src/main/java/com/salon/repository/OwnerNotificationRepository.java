package com.salon.repository;

import com.salon.entity.OwnerNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OwnerNotificationRepository extends JpaRepository<OwnerNotification, Long> {
    List<OwnerNotification> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<OwnerNotification> findByOwnerIdAndIsRead(Long ownerId, boolean isRead);
    long countByOwnerIdAndIsRead(Long ownerId, boolean isRead);
}
