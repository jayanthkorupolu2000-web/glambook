package com.salon.repository;

import com.salon.entity.ProfessionalAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProfessionalAvailabilityRepository extends JpaRepository<ProfessionalAvailability, Long> {
    List<ProfessionalAvailability> findByProfessionalId(Long professionalId);
    List<ProfessionalAvailability> findByProfessionalIdAndAvailDate(Long professionalId, LocalDate date);
    Optional<ProfessionalAvailability> findByProfessionalIdAndAvailDateAndStartTime(
            Long professionalId, LocalDate date, java.time.LocalTime startTime);
}
