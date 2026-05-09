package com.salon.service;

import com.salon.dto.response.ProfessionalNotificationResponse;
import com.salon.entity.ProfessionalNotificationType;

import java.util.List;

public interface ProfessionalNotificationService {
    void createNotification(Long professionalId, ProfessionalNotificationType type, Long referenceId, String message);
    List<ProfessionalNotificationResponse> getNotifications(Long professionalId);
    long getUnreadCount(Long professionalId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long professionalId);
}
