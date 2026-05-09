package com.salon.repository;

import com.salon.entity.Loyalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoyaltyRepository extends JpaRepository<Loyalty, Long> {
    Optional<Loyalty> findByCustomerIdAndOwnerId(Long customerId, Long ownerId);
    List<Loyalty> findByOwnerId(Long ownerId);
    List<Loyalty> findByCustomerId(Long customerId);
}
