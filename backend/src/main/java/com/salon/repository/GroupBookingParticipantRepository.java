package com.salon.repository;

import com.salon.entity.GroupBookingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupBookingParticipantRepository extends JpaRepository<GroupBookingParticipant, Long> {
    List<GroupBookingParticipant> findByGroupBookingId(Long groupBookingId);
    List<GroupBookingParticipant> findByCustomerId(Long customerId);
}
