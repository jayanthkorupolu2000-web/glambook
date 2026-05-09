package com.salon.repository;

import com.salon.entity.FavoriteService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteServiceRepository extends JpaRepository<FavoriteService, Long> {
    List<FavoriteService> findByCustomerId(Long customerId);
    Optional<FavoriteService> findByCustomerIdAndServiceId(Long customerId, Long serviceId);
    boolean existsByCustomerIdAndServiceId(Long customerId, Long serviceId);
    void deleteByCustomerIdAndServiceId(Long customerId, Long serviceId);
    void deleteByServiceId(Long serviceId);
}
