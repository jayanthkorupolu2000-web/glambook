package com.salon.service.impl;

import com.salon.dto.response.OwnerNotificationResponse;
import com.salon.entity.NotificationType;
import com.salon.entity.OwnerNotification;
import com.salon.entity.SalonOwner;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.OwnerNotificationRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.OwnerNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerNotificationServiceImpl implements OwnerNotificationService {

    private final OwnerNotificationRepository notificationRepository;
    private final SalonOwnerRepository salonOwnerRepository;

    @Override
    @Transactional
    public void createNotification(Long ownerId, NotificationType type, Long referenceId, String message) {
        SalonOwner owner = salonOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + ownerId));

        OwnerNotification notification = OwnerNotification.builder()
                .owner(owner)
                .type(type)
                .referenceId(referenceId)
                .message(message)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created for owner {}: {}", ownerId, message);
    }

    @Override
    public List<OwnerNotificationResponse> getNotificationsForOwner(Long ownerId) {
        return notificationRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long ownerId) {
        return notificationRepository.countByOwnerIdAndIsRead(ownerId, false);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long ownerId) {
        List<OwnerNotification> unread = notificationRepository.findByOwnerIdAndIsRead(ownerId, false);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private OwnerNotificationResponse toResponse(OwnerNotification n) {
        OwnerNotificationResponse res = new OwnerNotificationResponse();
        res.setId(n.getId());
        res.setType(n.getType().name());
        res.setReferenceId(n.getReferenceId());
        res.setMessage(n.getMessage());
        res.setRead(n.isRead());
        res.setCreatedAt(n.getCreatedAt());
        return res;
    }
}
