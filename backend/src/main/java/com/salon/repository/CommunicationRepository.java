package com.salon.repository;

import com.salon.entity.Communication;
import com.salon.entity.CommunicationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunicationRepository extends JpaRepository<Communication, Long> {
    List<Communication> findByProfessionalIdAndCustomerIdOrderByCreatedAtDesc(Long professionalId, Long customerId);
    List<Communication> findByProfessionalIdAndType(Long professionalId, CommunicationType type);
    List<Communication> findByCustomerIdAndIsRead(Long customerId, boolean isRead);
    long countByCustomerIdAndIsRead(Long customerId, boolean isRead);
}
