package com.salon.service;

import com.salon.dto.response.AppointmentResponse;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.dto.response.SalonOwnerResponse;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.entity.UserStatus;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalonOwnerService {

    private final SalonOwnerRepository salonOwnerRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentService appointmentService;

    public SalonOwnerResponse getProfile(Long ownerId) {
        log.info("Fetching profile for salon owner: {}", ownerId);
        SalonOwner owner = salonOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon owner not found with id: " + ownerId));
        return mapToSalonOwnerResponse(owner);
    }

    public List<ProfessionalResponse> getStaff(Long ownerId) {
        log.info("Fetching staff for salon owner: {}", ownerId);
        if (!salonOwnerRepository.existsById(ownerId)) {
            throw new ResourceNotFoundException("Salon owner not found with id: " + ownerId);
        }
        List<Professional> professionals = professionalRepository.findBySalonOwnerId(ownerId);
        return professionals.stream()
                .map(this::mapToProfessionalResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAppointments(Long ownerId) {
        log.info("Fetching appointments for salon owner: {}", ownerId);
        if (!salonOwnerRepository.existsById(ownerId)) {
            throw new ResourceNotFoundException("Salon owner not found with id: " + ownerId);
        }
        return appointmentService.getAppointmentsBySalonOwner(ownerId);
    }

    @Transactional
    public ProfessionalResponse approveProfessional(Long ownerId, Long professionalId) {
        SalonOwner owner = salonOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon owner not found: " + ownerId));
        Professional prof = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));
        if (prof.getSalonOwner() == null || !prof.getSalonOwner().getId().equals(ownerId)) {
            throw new InvalidOperationException("This professional does not belong to your salon");
        }
        prof.setStatus(UserStatus.ACTIVE);
        prof.setApprovedBy(owner);
        prof.setApprovedAt(LocalDateTime.now());
        return mapToProfessionalResponse(professionalRepository.save(prof));
    }

    @Transactional
    public ProfessionalResponse rejectProfessional(Long ownerId, Long professionalId) {
        Professional prof = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));
        if (prof.getSalonOwner() == null || !prof.getSalonOwner().getId().equals(ownerId)) {
            throw new InvalidOperationException("This professional does not belong to your salon");
        }
        prof.setStatus(UserStatus.SUSPENDED);
        return mapToProfessionalResponse(professionalRepository.save(prof));
    }

    private SalonOwnerResponse mapToSalonOwnerResponse(SalonOwner owner) {
        return SalonOwnerResponse.builder()
                .id(owner.getId())
                .name(owner.getName())
                .salonName(owner.getSalonName())
                .city(owner.getCity())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .build();
    }

    private ProfessionalResponse mapToProfessionalResponse(Professional professional) {
        SalonOwnerResponse ownerResponse = professional.getSalonOwner() != null
                ? mapToSalonOwnerResponse(professional.getSalonOwner())
                : null;
        return ProfessionalResponse.builder()
                .id(professional.getId())
                .name(professional.getName())
                .email(professional.getEmail())
                .city(professional.getCity())
                .specialization(professional.getSpecialization())
                .experienceYears(professional.getExperienceYears())
                .status(professional.getStatus() != null ? professional.getStatus().name() : "PENDING")
                .salonOwner(ownerResponse)
                .build();
    }
}
