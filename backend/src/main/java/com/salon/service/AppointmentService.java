package com.salon.service;

import com.salon.dto.request.AppointmentRequest;
import com.salon.dto.request.UpdateAppointmentStatusRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.dto.response.CustomerResponse;
import com.salon.dto.response.PaymentResponse;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.dto.response.SalonOwnerResponse;
import com.salon.dto.response.ServiceResponse;
import com.salon.entity.*;
import com.salon.exception.ConflictException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.UnauthorizedException;
import com.salon.exception.ValidationException;
import com.salon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRepository serviceRepository;
    private final ProfessionalAvailabilityRepository availabilityRepository;
    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        log.info("Creating appointment for customer: {}, professional: {}", 
                request.getCustomerId(), request.getProfessionalId());

        // Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Validate professional exists
        Professional professional = professionalRepository.findById(request.getProfessionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        // Validate service exists
        com.salon.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Check for time slot conflict using service duration
        int durationMins = service.getDurationMins() != null ? service.getDurationMins() : 30;
        LocalDate apptDate = request.getDateTime().toLocalDate();
        LocalTime apptStart = request.getDateTime().toLocalTime();
        LocalTime apptEnd = apptStart.plusMinutes(durationMins);

        // Check if any existing confirmed/pending appointment overlaps this time window
        boolean hasConflict = appointmentRepository
                .findByProfessionalIdAndDateTimeBetween(
                        request.getProfessionalId(),
                        request.getDateTime().toLocalDate().atStartOfDay(),
                        request.getDateTime().toLocalDate().atTime(23, 59))
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                .anyMatch(a -> {
                    LocalTime existStart = a.getDateTime().toLocalTime();
                    int existDur = a.getService() != null && a.getService().getDurationMins() != null
                            ? a.getService().getDurationMins() : 30;
                    LocalTime existEnd = existStart.plusMinutes(existDur);
                    return apptStart.isBefore(existEnd) && apptEnd.isAfter(existStart);
                });

        if (hasConflict) {
            throw new ConflictException("This time slot is already booked. Please choose a different time.");
        }

        // Check if the selected slot overlaps any break/lunch/blocked slot
        boolean overlapsBreak = availabilityRepository
                .findByProfessionalIdAndAvailDate(request.getProfessionalId(), apptDate)
                .stream()
                .filter(s -> s.getSlotType() != null
                        && s.getSlotType() != com.salon.entity.SlotType.WORKING)
                .anyMatch(s -> {
                    LocalTime bStart = s.getStartTime();
                    LocalTime bEnd = s.getEndTime();
                    return apptStart.isBefore(bEnd) && apptEnd.isAfter(bStart);
                });

        if (overlapsBreak) {
            throw new ConflictException(
                "The selected time slot overlaps a break or lunch period. " +
                "Please choose a time that doesn't overlap with scheduled breaks.");
        }
        availabilityRepository
                .findByProfessionalIdAndAvailDateAndStartTime(
                        request.getProfessionalId(), apptDate, apptStart)
                .ifPresent(slot -> {
                    slot.setBooked(true);
                    availabilityRepository.save(slot);
                });

        // Create appointment
        Appointment appointment = Appointment.builder()
                .customer(customer)
                .professional(professional)
                .service(service)
                .dateTime(request.getDateTime())
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Created appointment with ID: {}", savedAppointment.getId());

        return mapToAppointmentResponse(savedAppointment);
    }

    public List<AppointmentResponse> getAppointmentsByCustomer(Long customerId) {
        log.info("Fetching appointments for customer: {}", customerId);
        
        List<Appointment> appointments = appointmentRepository.findByCustomerId(customerId);
        return appointments.stream()
                .map(this::mapToAppointmentResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAppointmentsBySalonOwner(Long salonOwnerId) {
        log.info("Fetching appointments for salon owner: {}", salonOwnerId);
        
        List<Appointment> appointments = appointmentRepository.findByProfessionalSalonOwnerId(salonOwnerId);
        return appointments.stream()
                .map(this::mapToAppointmentResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAppointmentsByProfessional(Long professionalId) {
        log.info("Fetching appointments for professional: {}", professionalId);
        return appointmentRepository.findByProfessionalIdOrderByDateTimeDesc(professionalId)
                .stream().map(this::mapToAppointmentResponse).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse updateAppointmentStatus(Long appointmentId, UpdateAppointmentStatusRequest request) {
        log.info("Updating appointment {} status to: {}", appointmentId, request.getStatus());

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Validate status transition
        validateStatusTransition(appointment.getStatus(), request.getStatus());

        appointment.setStatus(request.getStatus());
        Appointment savedAppointment = appointmentRepository.save(appointment);

        log.info("Updated appointment {} status to: {}", appointmentId, request.getStatus());
        return mapToAppointmentResponse(savedAppointment);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        log.info("Cancelling appointment: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        log.info("Cancelled appointment: {}", appointmentId);
        return mapToAppointmentResponse(savedAppointment);
    }

    private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
        // Valid transitions: PENDING → CONFIRMED → COMPLETED; any → CANCELLED
        switch (currentStatus) {
            case PENDING:
                if (newStatus != AppointmentStatus.CONFIRMED && newStatus != AppointmentStatus.CANCELLED) {
                    throw new ValidationException("Invalid status transition from PENDING to " + newStatus);
                }
                break;
            case CONFIRMED:
                if (newStatus != AppointmentStatus.COMPLETED && newStatus != AppointmentStatus.CANCELLED) {
                    throw new ValidationException("Invalid status transition from CONFIRMED to " + newStatus);
                }
                break;
            case COMPLETED:
                if (newStatus != AppointmentStatus.CANCELLED) {
                    throw new ValidationException("Invalid status transition from COMPLETED to " + newStatus);
                }
                break;
            case CANCELLED:
                throw new ValidationException("Cannot change status of a cancelled appointment");
        }
    }

    private AppointmentResponse mapToAppointmentResponse(Appointment appointment) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(appointment.getId());
        res.setStatus(appointment.getStatus().name());
        res.setScheduledAt(appointment.getDateTime());
        if (appointment.getCustomer() != null) {
            res.setCustomerId(appointment.getCustomer().getId());
            res.setCustomerName(appointment.getCustomer().getName());
        }
        if (appointment.getProfessional() != null) {
            res.setProfessionalId(appointment.getProfessional().getId());
            res.setProfessionalName(appointment.getProfessional().getName());
            res.setProfessionalPhotoUrl(appointment.getProfessional().getProfilePhotoUrl());
        }
        if (appointment.getService() != null) {
            res.setServiceId(appointment.getService().getId());
            res.setServiceName(appointment.getService().getName());
            res.setServicePrice(appointment.getService().getPrice());
        }
        // Include payment records so frontend can detect PAY_LATER_PENDING
        paymentRepository.findByAppointmentId(appointment.getId()).ifPresent(p -> {
            PaymentResponse pr = PaymentResponse.builder()
                    .id(p.getId())
                    .appointmentId(appointment.getId())
                    .amount(p.getAmount())
                    .method(p.getMethod() != null ? p.getMethod().name() : null)
                    .status(p.getStatus() != null ? p.getStatus().name() : null)
                    .paidAt(p.getPaidAt())
                    .build();
            res.setPayments(java.util.List.of(pr));
        });
        return res;
    }

    private CustomerResponse mapToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .city(customer.getCity())
                .build();
    }

    private ProfessionalResponse mapToProfessionalResponse(Professional professional) {
        return ProfessionalResponse.builder()
                .id(professional.getId())
                .name(professional.getName())
                .email(professional.getEmail())
                .city(professional.getCity())
                .specialization(professional.getSpecialization())
                .experienceYears(professional.getExperienceYears())
                .salonOwner(mapToSalonOwnerResponse(professional.getSalonOwner()))
                .build();
    }

    private SalonOwnerResponse mapToSalonOwnerResponse(SalonOwner salonOwner) {
        if (salonOwner == null) {
            return null;
        }
        return SalonOwnerResponse.builder()
                .id(salonOwner.getId())
                .name(salonOwner.getName())
                .salonName(salonOwner.getSalonName())
                .city(salonOwner.getCity())
                .email(salonOwner.getEmail())
                .phone(salonOwner.getPhone())
                .build();
    }

    private ServiceResponse mapToServiceResponse(com.salon.entity.Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory())
                .gender(service.getGender().name())
                .price(service.getPrice())
                .durationMins(service.getDurationMins())
                .build();
    }
}