package com.salon.service.impl;

import com.salon.dto.request.PolicyRequest;
import com.salon.dto.response.PolicyResponse;
import com.salon.entity.Policy;
import com.salon.exception.PolicyNotFoundException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.AdminRepository;
import com.salon.repository.PolicyRepository;
import com.salon.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final AdminRepository adminRepository;

    @Override
    public PolicyResponse publishPolicy(Long adminId, PolicyRequest dto) {
        var admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

        Policy policy = Policy.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .publishedBy(admin)
                .build();

        Policy saved = policyRepository.save(policy);
        log.info("Policy '{}' published by admin {}", saved.getTitle(), adminId);
        return toResponse(saved);
    }

    @Override
    public PolicyResponse getLatestPolicy() {
        return policyRepository.findTopByOrderByCreatedAtDesc()
                .map(this::toResponse)
                .orElseThrow(() -> new PolicyNotFoundException("No policies found"));
    }

    @Override
    public List<PolicyResponse> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PolicyResponse toResponse(Policy p) {
        PolicyResponse res = new PolicyResponse();
        res.setId(p.getId());
        res.setTitle(p.getTitle());
        res.setContent(p.getContent());
        res.setPublishedBy(p.getPublishedBy().getUsername());
        res.setCreatedAt(p.getCreatedAt());
        return res;
    }
}
