package com.salon.service;

import com.salon.dto.request.SuspendProfessionalRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.dto.response.SalonOwnerResponse;
import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import com.salon.entity.Customer;
import com.salon.entity.CustomerNotificationType;
import com.salon.entity.Professional;
import com.salon.entity.ProfessionalNotificationType;
import com.salon.entity.SalonOwner;
import com.salon.entity.UserStatus;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalonOwnerService {

    private final SalonOwnerRepository salonOwnerRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalNotificationService professionalNotificationService;
    private final CustomerNotificationService customerNotificationService;

    public SalonOwnerResponse getProfile(Long ownerId) {
        log.info("Fetching profile for salon owner: {}", ownerId);
        SalonOwner owner = salonOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon owner not found with id: " + ownerId));
        return mapToSalonOwnerResponse(owner);
    }

    @Transactional
    public SalonOwnerResponse updateProfile(Long ownerId, com.salon.dto.request.SalonOwnerEditRequest dto) {
        SalonOwner owner = salonOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon owner not found with id: " + ownerId));
        owner.setName(dto.getName().trim());
        owner.setPhone(dto.getPhone().trim());
        owner.setSalonName(dto.getSalonName().trim());
        return mapToSalonOwnerResponse(salonOwnerRepository.save(owner));
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

        // Capture status BEFORE changing it
        boolean wasSuspended = UserStatus.SUSPENDED.equals(prof.getStatus());

        prof.setStatus(UserStatus.ACTIVE);
        prof.setApprovedBy(owner);
        prof.setApprovedAt(LocalDateTime.now());
        prof.setSuspensionReason(null);
        prof.setSuspendedUntil(null);
        professionalRepository.save(prof);

        // Always send reactivation notification when approving a suspended professional
        if (wasSuspended) {
            log.info("Sending REACTIVATED notification to professional {}", professionalId);
            professionalNotificationService.createNotification(
                    professionalId,
                    ProfessionalNotificationType.REACTIVATED,
                    ownerId,
                    "✅ Your account has been reactivated by salon owner " + owner.getName()
                    + ". You can now accept bookings again. Welcome back!");
        } else {
            // First-time approval notification
            professionalNotificationService.createNotification(
                    professionalId,
                    ProfessionalNotificationType.APPOINTMENT_CONFIRMED,
                    ownerId,
                    "✅ Your account has been approved by salon owner " + owner.getName()
                    + ". You can now start accepting bookings!");
        }
        return mapToProfessionalResponse(prof);
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

    @Transactional
    public ProfessionalResponse suspendProfessional(Long ownerId, Long professionalId,
                                                     SuspendProfessionalRequest request) {
        SalonOwner owner = salonOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon owner not found: " + ownerId));
        Professional prof = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));
        if (prof.getSalonOwner() == null || !prof.getSalonOwner().getId().equals(ownerId)) {
            throw new InvalidOperationException("This professional does not belong to your salon");
        }

        // Set suspension fields
        prof.setStatus(UserStatus.SUSPENDED);
        prof.setSuspensionReason(request.getReason());
        LocalDateTime suspendedUntil = (request.getDurationDays() != null && request.getDurationDays() > 0)
                ? LocalDateTime.now().plusDays(request.getDurationDays())
                : null; // null = permanent
        prof.setSuspendedUntil(suspendedUntil);
        professionalRepository.save(prof);

        // Build notification message for professional
        String durationText = (suspendedUntil != null)
                ? "for " + request.getDurationDays() + " day(s) (until "
                  + suspendedUntil.toLocalDate() + ")"
                : "permanently";
        String profMessage = "Your account has been suspended " + durationText
                + " by salon owner " + owner.getName()
                + ". Reason: " + request.getReason();
        professionalNotificationService.createNotification(
                professionalId,
                ProfessionalNotificationType.SUSPENSION_NOTICE,
                ownerId,
                profMessage);

        // Cancel all active (PENDING / CONFIRMED) appointments and notify customers
        List<Appointment> activeAppointments = new ArrayList<>(appointmentRepository
                .findByProfessionalIdAndStatus(professionalId, AppointmentStatus.PENDING));
        activeAppointments.addAll(
                appointmentRepository.findByProfessionalIdAndStatus(professionalId, AppointmentStatus.CONFIRMED));

        for (Appointment appt : activeAppointments) {
            appt.setStatus(AppointmentStatus.CANCELLED);
            appt.setCancelledAt(LocalDateTime.now());
            appointmentRepository.save(appt);

            // Notify the customer
            if (appt.getCustomer() != null) {
                Customer customer = appt.getCustomer();
                String serviceName = appt.getService() != null ? appt.getService().getName() : "your service";
                String apptDate = appt.getDateTime() != null
                        ? appt.getDateTime().toLocalDate().toString() : "scheduled date";
                String customerMessage = "Your appointment for \"" + serviceName
                        + "\" on " + apptDate
                        + " with " + prof.getName()
                        + " has been cancelled because the professional is no longer available. "
                        + "We apologise for the inconvenience. Please rebook with another professional.";
                customerNotificationService.createNotification(
                        customer.getId(),
                        CustomerNotificationType.BOOKING_CANCELLED,
                        appt.getId(),
                        customerMessage);
            }
        }

        log.info("Professional {} suspended by owner {}. Cancelled {} appointments.",
                professionalId, ownerId, activeAppointments.size());

        return mapToProfessionalResponse(prof);
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
                .suspensionReason(professional.getSuspensionReason())
                .suspendedUntil(professional.getSuspendedUntil())
                .salonOwner(ownerResponse)
                .build();
    }
}
