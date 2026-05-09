package com.salon.repository;

import com.salon.entity.Professional;
import com.salon.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {
    Optional<Professional> findByEmail(String email);
    Page<Professional> findByCity(String city, Pageable pageable);
    List<Professional> findBySalonOwnerId(Long salonOwnerId);
    List<Professional> findBySalonOwnerIdAndStatus(Long ownerId, UserStatus status);
    long countBySalonOwnerIdAndStatus(Long ownerId, UserStatus status);
    List<Professional> findByCityIgnoreCase(String city);
}
