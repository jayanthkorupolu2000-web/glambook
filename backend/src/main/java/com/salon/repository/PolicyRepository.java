package com.salon.repository;

import com.salon.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findTopByOrderByCreatedAtDesc();
}
