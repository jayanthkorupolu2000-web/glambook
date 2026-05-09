package com.salon.repository;

import com.salon.entity.ProfessionalNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessionalNotificationRepository extends JpaRepository<ProfessionalNotification, Long> {
    List<ProfessionalNotification> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);
    List<ProfessionalNotification> findByProfessionalIdAndIsRead(Long professionalId, boolean isRead);
    long countByProfessionalIdAndIsRead(Long professionalId, boolean isRead);
}
