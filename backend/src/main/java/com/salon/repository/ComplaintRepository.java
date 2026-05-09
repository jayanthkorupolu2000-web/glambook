package com.salon.repository;

import com.salon.entity.Complaint;
import com.salon.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByProfessionalId(Long professionalId);

    List<Complaint> findByCustomerId(Long customerId);

    long countByProfessionalId(Long professionalId);

    long countByCustomerId(Long customerId);

    // Count open + forwarded complaints for a professional (used for auto-suspend)
    long countByProfessionalIdAndStatusIn(Long professionalId, List<ComplaintStatus> statuses);

    // Complaints per city via professional's city
    @Query("SELECT p.city, COUNT(c) FROM Complaint c JOIN c.professional p GROUP BY p.city")
    List<Object[]> countComplaintsByCity();

    // Ratings distribution
    @Query("SELECT c.rating, COUNT(c) FROM Complaint c GROUP BY c.rating")
    List<Object[]> countByRating();

    // Owner scope
    List<Complaint> findByProfessionalSalonOwnerIdAndStatus(Long ownerId, ComplaintStatus status);
    List<Complaint> findByProfessionalSalonOwnerId(Long ownerId);
    long countByProfessionalSalonOwnerIdAndStatus(Long ownerId, ComplaintStatus status);
}
