package com.salon.repository;

import com.salon.entity.SalonPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalonPolicyRepository extends JpaRepository<SalonPolicy, Long> {
    List<SalonPolicy> findByOwnerId(Long ownerId);
    Optional<SalonPolicy> findTopByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<SalonPolicy> findByOwnerCityIgnoreCaseOrderByCreatedAtDesc(String city);
}
