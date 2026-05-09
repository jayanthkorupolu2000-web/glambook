package com.salon.repository;

import com.salon.entity.ServiceBeforeAfter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceBeforeAfterRepository extends JpaRepository<ServiceBeforeAfter, Long> {
    List<ServiceBeforeAfter> findByServiceId(Long serviceId);
    List<ServiceBeforeAfter> findByProfessionalId(Long professionalId);
}
