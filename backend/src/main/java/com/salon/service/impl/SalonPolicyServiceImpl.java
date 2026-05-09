package com.salon.service.impl;

import com.salon.dto.request.SalonPolicyRequest;
import com.salon.dto.response.SalonPolicyResponse;
import com.salon.entity.*;
import com.salon.exception.PolicyNotFoundException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import com.salon.service.SalonPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalonPolicyServiceImpl implements SalonPolicyService {

    private final SalonPolicyRepository policyRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final ProfessionalRepository professionalRepository;
    private final ProfessionalNotificationRepository notificationRepository;

    @Override
    @Transactional
    public SalonPolicyResponse publishPolicy(SalonPolicyRequest dto) {
        SalonOwner owner = salonOwnerRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + dto.getOwnerId()));

        SalonPolicy policy = SalonPolicy.builder()
                .owner(owner)
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();

        SalonPolicy saved = policyRepository.save(policy);
        log.info("Policy '{}' published by owner {} ({})", saved.getTitle(), owner.getId(), owner.getCity());

        // Notify all professionals mapped to this salon owner
        notifyProfessionals(owner, saved);

        return toResponse(saved);
    }

    /**
     * Send a POLICY_PUBLISHED notification to every professional
     * who belongs to this salon owner.
     */
    private void notifyProfessionals(SalonOwner owner, SalonPolicy policy) {
        List<Professional> professionals = professionalRepository.findBySalonOwnerId(owner.getId());
        if (professionals.isEmpty()) {
            log.debug("No professionals found for owner {}", owner.getId());
            return;
        }

        String message = "📋 New Policy Published by " + owner.getSalonName()
                + ": " + policy.getTitle();

        List<ProfessionalNotification> notifications = professionals.stream()
                .map(pro -> ProfessionalNotification.builder()
                        .professional(pro)
                        .type(ProfessionalNotificationType.POLICY_PUBLISHED)
                        .referenceId(policy.getId())
                        .message(message)
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
        log.info("Sent POLICY_PUBLISHED notification to {} professionals for policy {}",
                notifications.size(), policy.getId());
    }

    @Override
    public List<SalonPolicyResponse> getPoliciesByOwner(Long ownerId) {
        return policyRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public SalonPolicyResponse getLatestPolicyByOwner(Long ownerId) {
        return policyRepository.findTopByOwnerIdOrderByCreatedAtDesc(ownerId)
                .map(this::toResponse)
                .orElseThrow(() -> new PolicyNotFoundException("No policies found for owner: " + ownerId));
    }

    @Override
    public List<SalonPolicyResponse> getPoliciesByCity(String city) {
        return policyRepository.findByOwnerCityIgnoreCaseOrderByCreatedAtDesc(city)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private SalonPolicyResponse toResponse(SalonPolicy p) {
        SalonPolicyResponse res = new SalonPolicyResponse();
        res.setId(p.getId());
        res.setOwnerId(p.getOwner().getId());
        res.setOwnerName(p.getOwner().getName());
        res.setTitle(p.getTitle());
        res.setContent(p.getContent());
        res.setCreatedAt(p.getCreatedAt());
        return res;
    }
}
