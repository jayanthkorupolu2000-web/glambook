package com.salon.controller;

import com.salon.dto.request.BookingAssignmentRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.dto.response.SalonOwnerResponse;
import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.security.JwtUtil;
import com.salon.service.SalonOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Salon Owner", description = "Salon owner management APIs")
public class SalonOwnerController {

    private final SalonOwnerService salonOwnerService;
    private final SalonOwnerRepository salonOwnerRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/me")
    @Operation(summary = "Get current owner ID from JWT token")
    public ResponseEntity<Map<String, Object>> getMe(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            SalonOwner owner = salonOwnerRepository.findByEmail(email).orElse(null);
            if (owner != null) {
                return ResponseEntity.ok(Map.of("id", owner.getId(), "name", owner.getName(), "email", owner.getEmail()));
            }
        }
        return ResponseEntity.status(404).body(Map.of("error", "Owner not found"));
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get salon owner profile",
               description = "Returns salon owner profile including name, salon name, city, email, and phone",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SalonOwnerResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(salonOwnerService.getProfile(id));
    }

    @GetMapping("/{id}/staff")
    @Operation(summary = "Get staff for salon owner",
               description = "Returns all professionals assigned to this salon owner",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ProfessionalResponse>> getStaff(@PathVariable Long id) {
        return ResponseEntity.ok(salonOwnerService.getStaff(id));
    }

    @GetMapping("/{id}/appointments")
    @Operation(summary = "Get appointments for salon",
               description = "Returns all appointments for the salon owner's professionals",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<AppointmentResponse>> getAppointments(@PathVariable Long id) {
        return ResponseEntity.ok(salonOwnerService.getAppointments(id));
    }

    @PatchMapping("/{ownerId}/staff/{professionalId}/approve")
    @Operation(summary = "Approve a professional",
               description = "Salon owner approves a PENDING professional, setting status to ACTIVE",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProfessionalResponse> approveProfessional(
            @PathVariable Long ownerId,
            @PathVariable Long professionalId) {
        return ResponseEntity.ok(salonOwnerService.approveProfessional(ownerId, professionalId));
    }

    @PatchMapping("/{ownerId}/staff/{professionalId}/reject")
    @Operation(summary = "Reject a professional",
               description = "Salon owner rejects a PENDING professional, setting status to SUSPENDED",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProfessionalResponse> rejectProfessional(
            @PathVariable Long ownerId,
            @PathVariable Long professionalId) {
        return ResponseEntity.ok(salonOwnerService.rejectProfessional(ownerId, professionalId));
    }

    @PatchMapping("/appointments/{appointmentId}/assign")
    @Operation(summary = "Assign a professional to a PENDING appointment and confirm it",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AppointmentResponse> assignProfessional(
            @PathVariable Long appointmentId,
            @Valid @RequestBody BookingAssignmentRequest request) {

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (appt.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidOperationException("Only PENDING appointments can be assigned");
        }

        Professional prof = professionalRepository.findById(request.getProfessionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + request.getProfessionalId()));

        appt.setProfessional(prof);
        appt.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appt);

        AppointmentResponse res = new AppointmentResponse();
        res.setId(saved.getId());
        res.setStatus(saved.getStatus().name());
        res.setScheduledAt(saved.getDateTime());
        if (saved.getCustomer() != null) {
            res.setCustomerId(saved.getCustomer().getId());
            res.setCustomerName(saved.getCustomer().getName());
        }
        res.setProfessionalId(prof.getId());
        res.setProfessionalName(prof.getName());
        if (saved.getService() != null) {
            res.setServiceId(saved.getService().getId());
            res.setServiceName(saved.getService().getName());
        }
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/appointments/{appointmentId}/unassign")
    @Operation(summary = "Cancel assignment — removes professional and reverts appointment to PENDING",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AppointmentResponse> unassignProfessional(@PathVariable Long appointmentId) {

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidOperationException("Only CONFIRMED appointments can have their assignment cancelled");
        }

        appt.setProfessional(null);
        appt.setStatus(AppointmentStatus.PENDING);
        Appointment saved = appointmentRepository.save(appt);

        AppointmentResponse res = new AppointmentResponse();
        res.setId(saved.getId());
        res.setStatus(saved.getStatus().name());
        res.setScheduledAt(saved.getDateTime());
        if (saved.getCustomer() != null) {
            res.setCustomerId(saved.getCustomer().getId());
            res.setCustomerName(saved.getCustomer().getName());
        }
        res.setProfessionalId(null);
        res.setProfessionalName(null);
        if (saved.getService() != null) {
            res.setServiceId(saved.getService().getId());
            res.setServiceName(saved.getService().getName());
        }
        return ResponseEntity.ok(res);
    }
}
