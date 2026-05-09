package com.salon.repository;

import com.salon.entity.ResourceAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ResourceAvailabilityRepository extends JpaRepository<ResourceAvailability, Long> {
    List<ResourceAvailability> findByResourceId(Long resourceId);
    Optional<ResourceAvailability> findByResourceIdAndAvailDateAndStartTime(Long resourceId, LocalDate date, LocalTime startTime);
    List<ResourceAvailability> findByResourceIdAndAvailDateAndIsBooked(Long resourceId, LocalDate date, boolean isBooked);
}
