package com.salon.service.impl;

import com.salon.dto.response.ProfessionalNotificationResponse;
import com.salon.entity.Professional;
import com.salon.entity.ProfessionalNotification;
import com.salon.entity.ProfessionalNotificationType;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.ProfessionalNotificationRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.service.ProfessionalNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalNotificationServiceImpl implements ProfessionalNotificationService {

    private final ProfessionalNotificationRepository notifRepo;
    private final ProfessionalRepository professionalRepository;

    @Override
    @Transactional
    public void createNotification(Long professionalId, ProfessionalNotificationType type, Long referenceId, String message) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));

        ProfessionalNotification notif = ProfessionalNotification.builder()
                .professional(professional)
                .type(type)
                .referenceId(referenceId)
                .message(message)
                .build();
        notifRepo.save(notif);
    }

    @Override
    public List<ProfessionalNotificationResponse> getNotifications(Long professionalId) {
        return notifRepo.findByProfessionalIdOrderByCreatedAtDesc(professionalId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long professionalId) {
        return notifRepo.countByProfessionalIdAndIsRead(professionalId, false);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notifRepo.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notifRepo.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long professionalId) {
        List<ProfessionalNotification> unread = notifRepo.findByProfessionalIdAndIsRead(professionalId, false);
        unread.forEach(n -> n.setRead(true));
        notifRepo.saveAll(unread);
    }

    private ProfessionalNotificationResponse toResponse(ProfessionalNotification n) {
        ProfessionalNotificationResponse res = new ProfessionalNotificationResponse();
        res.setId(n.getId());
        res.setType(n.getType().name());
        res.setReferenceId(n.getReferenceId());
        res.setMessage(n.getMessage());
        res.setRead(n.isRead());
        res.setCreatedAt(n.getCreatedAt());
        return res;
    }
}
