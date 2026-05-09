package com.salon.service;

import com.salon.dto.response.OwnerNotificationResponse;
import com.salon.entity.NotificationType;

import java.util.List;

public interface OwnerNotificationService {
    void createNotification(Long ownerId, NotificationType type, Long referenceId, String message);
    List<OwnerNotificationResponse> getNotificationsForOwner(Long ownerId);
    long getUnreadCount(Long ownerId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long ownerId);
}
