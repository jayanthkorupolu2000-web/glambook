package com.salon.repository;

import com.salon.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    boolean existsByReferenceId(Long referenceId);

    List<AdminNotification> findAllByOrderByCreatedAtDesc();
}
