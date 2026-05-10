package com.salon.service;

import com.salon.dto.response.UserStatusResponse;

import java.time.LocalDate;

public interface SuspensionService {
    void autoSuspendProfessionalIfNeeded(Long professionalId);
    void handleAppointmentCancellation(Long customerId, LocalDate appointmentDate);
    UserStatusResponse updateUserStatus(Long userId, String userType, String status);
    UserStatusResponse updateUserStatus(Long userId, String userType, String status, String reason);
}
