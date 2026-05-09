package com.salon.repository;

import com.salon.entity.Consultation;
import com.salon.entity.ConsultationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByCustomerId(Long customerId);
    List<Consultation> findByProfessionalId(Long professionalId);
    List<Consultation> findByProfessionalIdAndStatus(Long professionalId, ConsultationStatus status);

    @Query("SELECT c FROM Consultation c WHERE c.professional.salonOwner.id = :ownerId")
    List<Consultation> findBySalonOwnerId(@Param("ownerId") Long ownerId);
}
