package com.salon.service.impl;

import com.salon.dto.request.ComplaintRequest;
import com.salon.dto.request.MediationRequest;
import com.salon.dto.response.ComplaintResponse;
import com.salon.entity.*;
import com.salon.exception.ComplaintNotFoundException;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.AdminNotificationRepository;
import com.salon.repository.ComplaintRepository;
import com.salon.repository.CustomerRepository;
import com.salon.repository.OwnerNotificationRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.service.ComplaintService;
import com.salon.service.SuspensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final CustomerRepository customerRepository;
    private final ProfessionalRepository professionalRepository;
    private final SuspensionService suspensionService;
    private final AdminNotificationRepository adminNotificationRepository;
    private final OwnerNotificationRepository ownerNotificationRepository;

    @Override
    @Transactional
    public ComplaintResponse createComplaint(ComplaintRequest dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + dto.getCustomerId()));

        Professional professional = professionalRepository.findById(dto.getProfessionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found with id: " + dto.getProfessionalId()));

        if (professional.getStatus() == UserStatus.SUSPENDED) {
            throw new InvalidOperationException("Cannot file complaint against a suspended professional");
        }

        Complaint complaint = Complaint.builder()
                .customer(customer)
                .professional(professional)
                .description(dto.getDescription())
                .feedback(ComplaintFeedback.valueOf(dto.getFeedback()))
                .rating(dto.getRating())
                .status(ComplaintStatus.OPEN)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        log.info("Complaint created with id: {}", saved.getId());

        // Notify admin about new complaint
        adminNotificationRepository.save(AdminNotification.builder()
                .message("New complaint #" + saved.getId() + " filed by " + customer.getName()
                        + " against professional " + professional.getName()
                        + ". Feedback: " + dto.getFeedback() + ". Rating: " + dto.getRating() + "/5.")
                .referenceId(saved.getId())
                .build());

        // Check if professional should be auto-suspended
        suspensionService.autoSuspendProfessionalIfNeeded(professional.getId());

        return toResponse(saved);
    }

    @Override
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ComplaintResponse> getComplaintsByCustomer(Long customerId) {
        return complaintRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ComplaintResponse> getComplaintsByStatus(String status) {
        ComplaintStatus complaintStatus = ComplaintStatus.valueOf(status.toUpperCase());
        return complaintRepository.findByStatus(complaintStatus).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ComplaintResponse forwardComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found with id: " + complaintId));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new InvalidOperationException("Cannot forward an already resolved complaint");
        }

        complaint.setStatus(ComplaintStatus.FORWARDED);
        Complaint updated = complaintRepository.save(complaint);
        log.info("Complaint {} forwarded to salon owner", complaintId);

        // Notify the salon owner
        SalonOwner owner = complaint.getProfessional().getSalonOwner();
        if (owner != null) {
            ownerNotificationRepository.save(OwnerNotification.builder()
                    .owner(owner)
                    .type(NotificationType.COMPLAINT_FORWARDED)
                    .referenceId(complaintId)
                    .message("Complaint #" + complaintId + " has been forwarded to you by admin. "
                            + "Customer: " + complaint.getCustomer().getName()
                            + " | Professional: " + complaint.getProfessional().getName()
                            + " | Feedback: " + complaint.getFeedback().name()
                            + ". Please review and take corrective action.")
                    .build());
        }

        return toResponse(updated);
    }

    @Override
    @Transactional
    public ComplaintResponse mediateComplaint(Long complaintId, MediationRequest dto) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found with id: " + complaintId));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new InvalidOperationException("Complaint is already resolved");
        }

        complaint.setResolutionNotes(dto.getResolutionNotes());
        complaint.setStatus(ComplaintStatus.RESOLVED);
        Complaint updated = complaintRepository.save(complaint);
        log.info("Complaint {} resolved by admin", complaintId);
        return toResponse(updated);
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
