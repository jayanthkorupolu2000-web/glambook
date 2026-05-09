package com.salon.service.impl;

import com.salon.dto.response.AdminNotificationResponse;
import com.salon.entity.AdminNotification;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.AdminNotificationRepository;
import com.salon.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;

    @Override
    public List<AdminNotificationResponse> getAll() {
        return adminNotificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        AdminNotification notification = adminNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin notification not found: " + id));
        notification.setRead(true);
        adminNotificationRepository.save(notification);
        log.info("Admin notification {} marked as read", id);
    }

    private AdminNotificationResponse toResponse(AdminNotification n) {
        return AdminNotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
