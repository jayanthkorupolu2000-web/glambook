package com.salon.repository;

import com.salon.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Customer> findByReferralCode(String referralCode);
    List<Customer> findByCity(String city);
    long countByStatus(com.salon.entity.UserStatus status);
}
