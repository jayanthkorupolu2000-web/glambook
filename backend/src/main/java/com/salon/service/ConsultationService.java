package com.salon.service;

import com.salon.dto.request.ConsultationReplyRequest;
import com.salon.dto.request.ConsultationRequest;
import com.salon.dto.response.ConsultationResponse;
import com.salon.entity.*;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final CustomerRepository customerRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public ConsultationResponse create(ConsultationRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidOperationException("Customer account is not active");
        }

        Consultation.ConsultationBuilder builder = Consultation.builder()
                .customer(customer)
                .question(req.getQuestion())
                .status(ConsultationStatus.PENDING);

        try { builder.type(ConsultationType.valueOf(req.getType())); }
        catch (Exception e) { builder.type(ConsultationType.GENERAL); }

        try { builder.topic(ConsultationTopic.valueOf(req.getTopic())); }
        catch (Exception e) { builder.topic(ConsultationTopic.GENERAL); }

        if (req.getProfessionalId() != null) {
            Professional pro = professionalRepository.findById(req.getProfessionalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
            builder.professional(pro);
        }

        if (req.getAppointmentId() != null) {
            appointmentRepository.findById(req.getAppointmentId())
                    .ifPresent(builder::appointment);
        }

        Consultation saved = consultationRepository.save(builder.build());
        log.info("Created consultation {} for customer {}", saved.getId(), customer.getId());
        return toResponse(saved);
    }

    public List<ConsultationResponse> getByCustomer(Long customerId) {
        return consultationRepository.findByCustomerId(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ConsultationResponse> getByProfessional(Long professionalId, ConsultationStatus status) {
        List<Consultation> list = status != null
                ? consultationRepository.findByProfessionalIdAndStatus(professionalId, status)
                : consultationRepository.findByProfessionalId(professionalId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ConsultationResponse> getBySalonOwner(Long ownerId) {
        return consultationRepository.findBySalonOwnerId(ownerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ConsultationResponse> getAll() {
        return consultationRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void attachPhoto(Long consultationId, String photoUrl) {
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));
        c.setPhotoUrl(photoUrl);
        consultationRepository.save(c);
    }

    @Transactional
    public ConsultationResponse reply(Long professionalId, Long consultationId, ConsultationReplyRequest req) {
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));

        if (c.getProfessional() == null || !c.getProfessional().getId().equals(professionalId)) {
            throw new AccessDeniedException("This consultation is not assigned to you");
        }

        if (c.getStatus() == ConsultationStatus.CLOSED) {
            throw new InvalidOperationException("Consultation is already closed");
        }

        c.setNotes(req.getNotes());
        c.setStatus(ConsultationStatus.RESPONDED);
        return toResponse(consultationRepository.save(c));
    }

    private ConsultationResponse toResponse(Consultation c) {
        ConsultationResponse r = new ConsultationResponse();
        r.setId(c.getId());
        r.setQuestion(c.getQuestion());
        r.setNotes(c.getNotes());
        r.setPhotoUrl(c.getPhotoUrl());
        r.setStatus(c.getStatus().name());
        r.setType(c.getType() != null ? c.getType().name() : null);
        r.setTopic(c.getTopic() != null ? c.getTopic().name() : null);
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        if (c.getCustomer() != null) {
            r.setCustomerId(c.getCustomer().getId());
            r.setCustomerName(c.getCustomer().getName());
        }
        if (c.getProfessional() != null) {
            r.setProfessionalId(c.getProfessional().getId());
            r.setProfessionalName(c.getProfessional().getName());
        }
        return r;
    }
}
