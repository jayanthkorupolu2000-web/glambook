package com.salon.repository;

import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByCustomerId(Long customerId);
    List<Appointment> findByProfessionalSalonOwnerId(Long salonOwnerId);
    boolean existsByProfessionalIdAndDateTime(Long professionalId, LocalDateTime dateTime);

    long countByCancelledAtIsNotNull();

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.cancelledAt IS NOT NULL AND DATE(a.cancelledAt) = DATE(a.dateTime)")
    long countSameDayCancellations();

    List<Appointment> findByProfessionalSalonOwnerIdAndStatus(Long ownerId, AppointmentStatus status);
    long countByProfessionalSalonOwnerIdAndStatus(Long ownerId, AppointmentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.appointment.professional.salonOwner.id = :ownerId AND p.status = 'PAID'")
    BigDecimal sumRevenueByOwnerId(@Param("ownerId") Long ownerId);

    List<Appointment> findByProfessionalIdAndStatus(Long professionalId, AppointmentStatus status);
    long countByProfessionalIdAndStatus(Long professionalId, AppointmentStatus status);
    List<Appointment> findByProfessionalIdOrderByDateTimeDesc(Long professionalId);

    @Query("SELECT a FROM Appointment a WHERE a.professional.id = :professionalId AND a.dateTime BETWEEN :from AND :to")
    List<Appointment> findByProfessionalIdAndDateTimeBetween(@Param("professionalId") Long professionalId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Customer dashboard methods
    List<Appointment> findByCustomerIdOrderByDateTimeDesc(Long customerId);
    List<Appointment> findByCustomerIdAndStatus(Long customerId, AppointmentStatus status);
    List<Appointment> findByCustomerIdAndDateTimeAfter(Long customerId, LocalDateTime now);

    @Query("SELECT a FROM Appointment a WHERE a.status = com.salon.entity.AppointmentStatus.CONFIRMED " +
           "AND a.dateTime < :now " +
           "AND NOT EXISTS (SELECT p FROM Payment p WHERE p.appointment = a AND p.status = com.salon.entity.PaymentStatus.PAID)")
    List<Appointment> findOverdueUnpaid(@Param("now") LocalDateTime now);
}
