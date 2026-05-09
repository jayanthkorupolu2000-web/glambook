package com.salon.repository;

import com.salon.entity.LoyaltyRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyRedemptionRepository extends JpaRepository<LoyaltyRedemption, Long> {
    List<LoyaltyRedemption> findByCustomerIdOrderByRedeemedAtDesc(Long customerId);
}
