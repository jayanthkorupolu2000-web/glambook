package com.salon.repository;

import com.salon.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
