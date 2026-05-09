package com.salon.service;

import com.salon.dto.response.AdminNotificationResponse;

import java.util.List;

public interface AdminNotificationService {
    List<AdminNotificationResponse> getAll();
    void markAsRead(Long id);
}
