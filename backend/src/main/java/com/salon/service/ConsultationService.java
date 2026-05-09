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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final CustomerRepository customerRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;

    /** Maps a ConsultationTopic to the category keywords used in the services table */
    private static final Map<ConsultationTopic, List<String>> TOPIC_KEYWORDS = Map.of(
        ConsultationTopic.HAIR,    List.of("hair"),
        ConsultationTopic.SKIN,    List.of("skin", "facial", "face"),
        ConsultationTopic.MAKEUP,  List.of("makeup", "cosmetic", "bridal"),
        ConsultationTopic.GENERAL, List.of()   // GENERAL → no filter, all professionals
    );

    @Transactional
    public ConsultationResponse create(ConsultationRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidOperationException("Customer account is not active");
        }

        ConsultationTopic topic;
        try { topic = ConsultationTopic.valueOf(req.getTopic()); }
        catch (Exception e) { topic = ConsultationTopic.GENERAL; }

        Consultation.ConsultationBuilder builder = Consultation.builder()
                .customer(customer)
                .question(req.getQuestion())
                .topic(topic)
                .status(ConsultationStatus.PENDING);

        if (req.getProfessionalId() != null) {
            // Specific professional chosen — assign directly
            Professional pro = professionalRepository.findById(req.getProfessionalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
            builder.professional(pro);
        }
        // If no professional chosen, leave professional null — visible to all matching professionals

        if (req.getAppointmentId() != null) {
            appointmentRepository.findById(req.getAppointmentId())
                    .ifPresent(builder::appointment);
        }

        Consultation saved = consultationRepository.save(builder.build());
        log.info("Created consultation {} for customer {} (topic={}, professional={})",
                saved.getId(), customer.getId(), topic,
                req.getProfessionalId() != null ? req.getProfessionalId() : "broadcast");
        return toResponse(saved);
    }

    public List<ConsultationResponse> getByCustomer(Long customerId) {
        return consultationRepository.findByCustomerId(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Returns consultations visible to a professional:
     * - Directly assigned to them, OR
     * - Unassigned (broadcast) and matching their service topic categories
     */
    public List<ConsultationResponse> getByProfessional(Long professionalId, ConsultationStatus status) {
        // Determine which topics this professional covers based on their services
        Set<ConsultationTopic> coveredTopics = getTopicsCoveredByProfessional(professionalId);

        Set<Long> seen = new LinkedHashSet<>();
        List<Consultation> result = new ArrayList<>();

        for (ConsultationTopic topic : coveredTopics) {
            List<Consultation> list = status != null
                    ? consultationRepository.findByProfessionalIdOrUnassignedByTopicAndStatus(professionalId, topic, status)
                    : consultationRepository.findByProfessionalIdOrUnassignedByTopic(professionalId, topic);
            for (Consultation c : list) {
                if (seen.add(c.getId())) result.add(c);
            }
        }

        // If professional covers no specific topics (or has no services), fall back to directly assigned only
        if (coveredTopics.isEmpty()) {
            List<Consultation> direct = status != null
                    ? consultationRepository.findByProfessionalIdAndStatus(professionalId, status)
                    : consultationRepository.findByProfessionalId(professionalId);
            direct.forEach(c -> { if (seen.add(c.getId())) result.add(c); });
        }

        // Always include GENERAL unassigned consultations
        List<Consultation> general = status != null
                ? consultationRepository.findByProfessionalIdOrUnassignedByTopicAndStatus(professionalId, ConsultationTopic.GENERAL, status)
                : consultationRepository.findByProfessionalIdOrUnassignedByTopic(professionalId, ConsultationTopic.GENERAL);
        general.forEach(c -> { if (seen.add(c.getId())) result.add(c); });

        result.sort(Comparator.comparing(Consultation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Returns professionals who have active services matching the given topic's categories */
    public List<com.salon.dto.response.ProfessionalResponse> getProfessionalsByTopic(String topicStr) {
        ConsultationTopic topic;
        try { topic = ConsultationTopic.valueOf(topicStr.toUpperCase()); }
        catch (Exception e) { topic = ConsultationTopic.GENERAL; }

        List<String> keywords = TOPIC_KEYWORDS.getOrDefault(topic, List.of());

        Set<Long> matchingProfIds = new LinkedHashSet<>();
        if (keywords.isEmpty()) {
            // GENERAL — return all active professionals
            professionalRepository.findAll().stream()
                    .filter(p -> p.getStatus() == UserStatus.ACTIVE)
                    .map(com.salon.entity.Professional::getId)
                    .forEach(matchingProfIds::add);
        } else {
            for (String kw : keywords) {
                matchingProfIds.addAll(serviceRepository.findProfessionalIdsByCategoryKeyword(kw));
            }
        }

        return matchingProfIds.stream()
                .map(id -> professionalRepository.findById(id).orElse(null))
                .filter(p -> p != null && p.getStatus() == UserStatus.ACTIVE)
                .map(this::toProfessionalResponse)
                .collect(Collectors.toList());
    }

    private Set<ConsultationTopic> getTopicsCoveredByProfessional(Long professionalId) {
        List<com.salon.entity.Service> services = serviceRepository.findByProfessionalId(professionalId);
        Set<ConsultationTopic> topics = new LinkedHashSet<>();
        for (com.salon.entity.Service svc : services) {
            if (svc.getCategory() == null) continue;
            String cat = svc.getCategory().toLowerCase();
            if (cat.contains("hair"))                                    topics.add(ConsultationTopic.HAIR);
            if (cat.contains("skin") || cat.contains("facial") || cat.contains("face")) topics.add(ConsultationTopic.SKIN);
            if (cat.contains("makeup") || cat.contains("cosmetic") || cat.contains("bridal")) topics.add(ConsultationTopic.MAKEUP);
        }
        return topics;
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

        // Allow reply if directly assigned OR if it's an unassigned broadcast consultation
        boolean isAssignedToMe = c.getProfessional() != null && c.getProfessional().getId().equals(professionalId);
        boolean isBroadcast = c.getProfessional() == null;

        if (!isAssignedToMe && !isBroadcast) {
            throw new AccessDeniedException("This consultation is not assigned to you");
        }

        if (c.getStatus() == ConsultationStatus.CLOSED) {
            throw new InvalidOperationException("Consultation is already closed");
        }

        // If broadcast, assign this professional as the responder
        if (isBroadcast) {
            Professional pro = professionalRepository.findById(professionalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
            c.setProfessional(pro);
        }

        c.setProfessionalReply(req.getProfessionalReply());
        c.setProfessionalRepliedAt(LocalDateTime.now());
        c.setStatus(ConsultationStatus.RESPONDED);
        return toResponse(consultationRepository.save(c));
    }

    private ConsultationResponse toResponse(Consultation c) {
        ConsultationResponse r = new ConsultationResponse();
        r.setId(c.getId());
        r.setQuestion(c.getQuestion());
        r.setNotes(c.getNotes());
        r.setProfessionalReply(c.getProfessionalReply());
        r.setProfessionalRepliedAt(c.getProfessionalRepliedAt());
        r.setPhotoUrl(c.getPhotoUrl());
        r.setStatus(c.getStatus().name());
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

    private com.salon.dto.response.ProfessionalResponse toProfessionalResponse(Professional p) {
        com.salon.dto.response.ProfessionalResponse r = new com.salon.dto.response.ProfessionalResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setSpecialization(p.getSpecialization());
        r.setCity(p.getCity());
        r.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        return r;
    }
}
