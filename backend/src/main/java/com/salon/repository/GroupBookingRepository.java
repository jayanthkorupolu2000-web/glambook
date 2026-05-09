package com.salon.repository;

import com.salon.entity.GroupBooking;
import com.salon.entity.GroupBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupBookingRepository extends JpaRepository<GroupBooking, Long> {
    List<GroupBooking> findByCustomerId(Long customerId);
    List<GroupBooking> findBySalonOwnerId(Long ownerId);
    List<GroupBooking> findByStatus(GroupBookingStatus status);
    List<GroupBooking> findByAppointmentsProfessionalId(Long professionalId);
}
