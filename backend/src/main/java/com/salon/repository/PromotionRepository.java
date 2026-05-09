package com.salon.repository;

import com.salon.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByOwnerId(Long ownerId);

    @Query("SELECT p FROM Promotion p WHERE p.owner.id = :ownerId AND p.isActive = true AND p.endDate >= CURRENT_DATE")
    List<Promotion> findActivePromotionsByOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.endDate < CURRENT_DATE")
    List<Promotion> findExpiredActivePromotions();
}
