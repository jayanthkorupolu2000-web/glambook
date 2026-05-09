package com.salon.service;

import com.salon.dto.response.CustomerNotificationResponse;
import com.salon.entity.CustomerNotificationType;

import java.util.List;

public interface CustomerNotificationService {
    void createNotification(Long customerId, CustomerNotificationType type, Long referenceId, String message);
    List<CustomerNotificationResponse> getNotifications(Long customerId);
    long getUnreadCount(Long customerId);
    void markAsRead(Long notificationId, Long customerId);
    void markAllAsRead(Long customerId);
}
