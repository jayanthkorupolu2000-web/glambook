package com.salon.repository;

import com.salon.entity.SalonOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonOwnerRepository extends JpaRepository<SalonOwner, Long> {
    Optional<SalonOwner> findByEmail(String email);
    Optional<SalonOwner> findByCity(String city);
}
