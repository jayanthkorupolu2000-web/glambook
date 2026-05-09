package com.salon.repository;

import com.salon.entity.Gender;
import com.salon.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByGender(Gender gender);
    List<Service> findByProfessionalId(Long professionalId);
    List<Service> findByNameContainingIgnoreCase(String name);
    java.util.Optional<Service> findFirstByNameIgnoreCase(String name);
    List<Service> findByCategoryIgnoreCase(String category);
}
