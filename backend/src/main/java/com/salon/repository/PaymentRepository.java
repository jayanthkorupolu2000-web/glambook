package com.salon.repository;

import com.salon.entity.Payment;
import com.salon.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByAppointmentId(Long appointmentId);
    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.appointment.professional.salonOwner.id = :ownerId AND p.status = 'PAID'")
    BigDecimal sumRevenueByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.appointment.professional.id = :professionalId AND p.status = 'PAID'")
    BigDecimal sumEarningsByProfessionalId(@Param("professionalId") Long professionalId);

    @Query("SELECT p FROM Payment p WHERE p.appointment.professional.id = :id AND p.appointment.dateTime BETWEEN :from AND :to AND p.status = 'PAID'")
    List<Payment> findPaidPaymentsByProfessionalAndDateRange(@Param("id") Long id, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
