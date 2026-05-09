package com.salon.repository;

import com.salon.entity.Consultation;
import com.salon.entity.ConsultationStatus;
import com.salon.entity.ConsultationTopic;
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

    /** Unassigned consultations (no specific professional) for a given topic */
    @Query("SELECT c FROM Consultation c WHERE c.professional IS NULL AND c.topic = :topic")
    List<Consultation> findUnassignedByTopic(@Param("topic") ConsultationTopic topic);

    /** All consultations visible to a professional: directly assigned OR unassigned matching topic */
    @Query("SELECT c FROM Consultation c WHERE " +
           "(c.professional IS NOT NULL AND c.professional.id = :professionalId) " +
           "OR (c.professional IS NULL AND c.topic = :topic)")
    List<Consultation> findByProfessionalIdOrUnassignedByTopic(
            @Param("professionalId") Long professionalId,
            @Param("topic") ConsultationTopic topic);

    /** Same as above but filtered by status */
    @Query("SELECT c FROM Consultation c WHERE " +
           "((c.professional IS NOT NULL AND c.professional.id = :professionalId) " +
           "OR (c.professional IS NULL AND c.topic = :topic)) AND c.status = :status")
    List<Consultation> findByProfessionalIdOrUnassignedByTopicAndStatus(
            @Param("professionalId") Long professionalId,
            @Param("topic") ConsultationTopic topic,
            @Param("status") ConsultationStatus status);
}
