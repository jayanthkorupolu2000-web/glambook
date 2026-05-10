package com.salon.service.impl;

import com.salon.dto.request.MediationActionRequest;
import com.salon.dto.response.ComplaintResponse;
import com.salon.entity.Complaint;
import com.salon.entity.ComplaintStatus;
import com.salon.exception.ComplaintNotFoundException;
import com.salon.exception.InvalidOperationException;
import com.salon.repository.ComplaintRepository;
import com.salon.service.ComplaintIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintIntegrationServiceImpl implements ComplaintIntegrationService {

    private final ComplaintRepository complaintRepository;

    @Override
    public List<ComplaintResponse> getForwardedComplaintsForOwner(Long ownerId) {
        return complaintRepository.findByProfessionalSalonOwnerIdAndStatus(ownerId, ComplaintStatus.FORWARDED)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ComplaintResponse> getAllComplaintsForOwner(Long ownerId) {
        return complaintRepository.findByProfessionalSalonOwnerId(ownerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ComplaintResponse logOwnerAction(Long ownerId, Long complaintId, MediationActionRequest dto) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found: " + complaintId));

        if (!complaint.getProfessional().getSalonOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("This complaint does not belong to your salon");
        }

        if (complaint.getStatus() != ComplaintStatus.FORWARDED) {
            throw new InvalidOperationException("Can only log action on FORWARDED complaints");
        }

        complaint.setOwnerActionNotes(dto.getOwnerActionNotes());
        complaint.setOwnerActionAt(LocalDateTime.now());

        return toResponse(complaintRepository.save(complaint));
    }

    @Override
    public ComplaintResponse getComplaintById(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found: " + complaintId));
        return toResponse(complaint);
    }

    private ComplaintResponse toResponse(Complaint c) {
        ComplaintResponse res = new ComplaintResponse();
        res.setId(c.getId());
        res.setCustomerId(c.getCustomer().getId());
        res.setCustomerName(c.getCustomer().getName());
        res.setProfessionalId(c.getProfessional().getId());
        res.setProfessionalName(c.getProfessional().getName());
        res.setDescription(c.getDescription());
        res.setFeedback(c.getFeedback().name());
        res.setRating(c.getRating());
        res.setStatus(c.getStatus().name());
        res.setResolutionNotes(c.getResolutionNotes());
        res.setOwnerActionNotes(c.getOwnerActionNotes());
        res.setOwnerActionAt(c.getOwnerActionAt());
        res.setCreatedAt(c.getCreatedAt());
        return res;
    }
}
