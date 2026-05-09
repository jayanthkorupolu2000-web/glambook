package com.salon.repository;

import com.salon.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProfessionalId(Long professionalId);

    List<Review> findByCustomerId(Long customerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.professional.id = :professionalId")
    Double findAverageRatingByProfessionalId(@Param("professionalId") Long professionalId);

    long countByProfessionalId(Long professionalId);

    long countByCustomerIdAndProfessionalId(Long customerId, Long professionalId);

    // Direct FK-based methods now that appointment_id column exists
    boolean existsByCustomerIdAndAppointmentId(Long customerId, Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    Optional<Review> findByAppointmentId(Long appointmentId);
}
