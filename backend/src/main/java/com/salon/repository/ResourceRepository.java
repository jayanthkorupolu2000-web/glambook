package com.salon.repository;

import com.salon.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByOwnerId(Long ownerId);
    List<Resource> findByOwnerIdAndIsAvailable(Long ownerId, boolean isAvailable);
}
