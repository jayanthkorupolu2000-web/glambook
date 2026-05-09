package com.salon.service;

import com.salon.dto.request.GroupBookingRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.dto.response.GroupBookingResponse;
import com.salon.entity.*;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBookingService {

    private final GroupBookingRepository groupBookingRepository;
    private final CustomerRepository customerRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public GroupBookingResponse createGroupBooking(GroupBookingRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidOperationException("Customer account is not active");
        }

        if (req.getProfessionalIds() == null || req.getProfessionalIds().size() < 2) {
            throw new InvalidOperationException("Group booking requires at least 2 professionals");
        }

        com.salon.entity.Service service = null;
        if (req.getServiceId() != null) {
            service = serviceRepository.findById(req.getServiceId()).orElse(null);
        }

        // Build per-professional service map
        java.util.Map<Long, Long> profServiceMap = new java.util.HashMap<>();
        if (req.getParticipantServices() != null) {
            for (GroupBookingRequest.ParticipantService ps : req.getParticipantServices()) {
                if (ps.getProfessionalId() != null && ps.getServiceId() != null) {
                    profServiceMap.put(ps.getProfessionalId(), ps.getServiceId());
                }
            }
        }

        SalonOwner salonOwner = null;
        if (req.getSalonOwnerId() != null) {
            salonOwner = salonOwnerRepository.findById(req.getSalonOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Salon owner not found"));
        }

        GroupBooking groupBooking = GroupBooking.builder()
                .customer(customer)
                .salonOwner(salonOwner)
                .scheduledAt(req.getScheduledAt())
                .discountPct(req.getDiscountPct() != null ? req.getDiscountPct() : java.math.BigDecimal.ZERO)
                .notes(req.getNotes())
                .status(GroupBookingStatus.PENDING)
                .build();

        GroupBooking saved = groupBookingRepository.save(groupBooking);

        // Create one appointment per professional with their own service
        List<Appointment> appointments = new ArrayList<>();
        for (Long profId : req.getProfessionalIds()) {
            Professional professional = professionalRepository.findById(profId)
                    .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + profId));

            // Resolve service: per-professional map → fallback to single serviceId
            Long svcId = profServiceMap.getOrDefault(profId, req.getServiceId());
            com.salon.entity.Service profService = service;
            if (svcId != null && (profService == null || !profService.getId().equals(svcId))) {
                profService = serviceRepository.findById(svcId).orElse(profService);
            }

            Appointment appt = Appointment.builder()
                    .customer(customer)
                    .professional(professional)
                    .service(profService)
                    .dateTime(req.getScheduledAt())
                    .status(AppointmentStatus.PENDING)
                    .groupBooking(saved)
                    .build();
            appointments.add(appointmentRepository.save(appt));
        }

        saved.setAppointments(appointments);
        log.info("Created group booking {} with {} professionals", saved.getId(), appointments.size());
        return toResponse(saved);
    }

    public List<GroupBookingResponse> getByCustomer(Long customerId) {
        return groupBookingRepository.findByCustomerId(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<GroupBookingResponse> getBySalonOwner(Long ownerId) {
        return groupBookingRepository.findBySalonOwnerId(ownerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<GroupBookingResponse> getByProfessional(Long professionalId) {
        return groupBookingRepository.findByAppointmentsProfessionalId(professionalId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<GroupBookingResponse> getAll() {
        return groupBookingRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public GroupBookingResponse confirm(Long ownerId, Long groupBookingId) {
        GroupBooking gb = groupBookingRepository.findById(groupBookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Group booking not found"));
        if (gb.getStatus() != GroupBookingStatus.PENDING) {
            throw new InvalidOperationException("Only PENDING bookings can be confirmed");
        }
        gb.setStatus(GroupBookingStatus.CONFIRMED);
        gb.getAppointments().forEach(a -> a.setStatus(AppointmentStatus.CONFIRMED));
        return toResponse(groupBookingRepository.save(gb));
    }

    @Transactional
    public GroupBookingResponse complete(Long ownerId, Long groupBookingId) {
        GroupBooking gb = groupBookingRepository.findById(groupBookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Group booking not found"));
        if (gb.getStatus() != GroupBookingStatus.CONFIRMED) {
            throw new InvalidOperationException("Only CONFIRMED bookings can be completed");
        }
        gb.setStatus(GroupBookingStatus.COMPLETED);
        gb.getAppointments().forEach(a -> a.setStatus(AppointmentStatus.COMPLETED));
        return toResponse(groupBookingRepository.save(gb));
    }

    @Transactional
    public GroupBookingResponse cancel(Long customerId, Long groupBookingId) {
        GroupBooking gb = groupBookingRepository.findById(groupBookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Group booking not found"));
        if (!gb.getCustomer().getId().equals(customerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your booking");
        }
        if (gb.getStatus() == GroupBookingStatus.COMPLETED) {
            throw new InvalidOperationException("Completed bookings cannot be cancelled");
        }
        gb.setStatus(GroupBookingStatus.CANCELLED);
        gb.getAppointments().forEach(a -> a.setStatus(AppointmentStatus.CANCELLED));
        return toResponse(groupBookingRepository.save(gb));
    }

    private GroupBookingResponse toResponse(GroupBooking gb) {
        GroupBookingResponse r = new GroupBookingResponse();
        r.setId(gb.getId());
        r.setStatus(gb.getStatus().name());
        r.setScheduledAt(gb.getScheduledAt());
        r.setDiscountPct(gb.getDiscountPct());
        r.setNotes(gb.getNotes());
        r.setCreatedAt(gb.getCreatedAt());
        if (gb.getCustomer() != null) {
            r.setCustomerId(gb.getCustomer().getId());
            r.setCustomerName(gb.getCustomer().getName());
        }
        if (gb.getSalonOwner() != null) {
            r.setSalonOwnerId(gb.getSalonOwner().getId());
            r.setSalonOwnerName(gb.getSalonOwner().getName());
        }
        if (gb.getAppointments() != null) {
            r.setAppointments(gb.getAppointments().stream().map(a -> {
                AppointmentResponse ar = new AppointmentResponse();
                ar.setId(a.getId());
                ar.setStatus(a.getStatus().name());
                ar.setScheduledAt(a.getDateTime());
                if (a.getProfessional() != null) {
                    ar.setProfessionalId(a.getProfessional().getId());
                    ar.setProfessionalName(a.getProfessional().getName());
                }
                if (a.getService() != null) {
                    ar.setServiceId(a.getService().getId());
                    ar.setServiceName(a.getService().getName());
                }
                return ar;
            }).collect(Collectors.toList()));
        }
        return r;
    }
}
