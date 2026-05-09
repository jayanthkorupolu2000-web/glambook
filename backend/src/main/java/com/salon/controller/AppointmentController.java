package com.salon.controller;

import com.salon.dto.request.AppointmentRequest;
import com.salon.dto.request.UpdateAppointmentStatusRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.exception.UnauthorizedException;
import com.salon.security.JwtUtil;
import com.salon.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Appointments", description = "Appointment management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @Operation(summary = "Create a new appointment", description = "Book an appointment with a professional for a specific service")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request,
            HttpServletRequest httpRequest) {
        log.info("Creating appointment request: {}", request);
        
        // Extract JWT token from Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid authorization header");
        }
        
        String token = authHeader.substring(7);
        String userRole = jwtUtil.extractRole(token);
        Long authenticatedUserId = jwtUtil.extractUserId(token);
        
        if (!"CUSTOMER".equals(userRole)) {
            throw new UnauthorizedException("Only customers can create appointments");
        }
        
        if (!authenticatedUserId.equals(request.getCustomerId())) {
            throw new UnauthorizedException("Cannot create appointment for another customer");
        }

        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get appointments", description = "Get appointments by customer ID or salon owner ID")
    public ResponseEntity<List<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salonOwnerId,
            @RequestParam(required = false) Long professionalId,
            HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid authorization header");
        }

        String token = authHeader.substring(7);
        String userRole = jwtUtil.extractRole(token);
        Long authenticatedUserId = jwtUtil.extractUserId(token);

        // Explicit professionalId param
        if (professionalId != null) {
            if ("PROFESSIONAL".equals(userRole) && !authenticatedUserId.equals(professionalId)) {
                throw new UnauthorizedException("Cannot access another professional's appointments");
            }
            return ResponseEntity.ok(appointmentService.getAppointmentsByProfessional(professionalId));
        }

        if (customerId != null && salonOwnerId != null) {
            throw new IllegalArgumentException("Cannot specify both customerId and salonOwnerId");
        }

        // If no params, use the JWT userId for the current user's role
        if (customerId == null && salonOwnerId == null) {
            if ("CUSTOMER".equals(userRole)) {
                return ResponseEntity.ok(appointmentService.getAppointmentsByCustomer(authenticatedUserId));
            } else if ("SALON_OWNER".equals(userRole)) {
                return ResponseEntity.ok(appointmentService.getAppointmentsBySalonOwner(authenticatedUserId));
            } else if ("PROFESSIONAL".equals(userRole)) {
                return ResponseEntity.ok(appointmentService.getAppointmentsByProfessional(authenticatedUserId));
            }
            throw new IllegalArgumentException("Must specify customerId or salonOwnerId");
        }

        if (customerId != null) {
            if ("CUSTOMER".equals(userRole) && !authenticatedUserId.equals(customerId)) {
                throw new UnauthorizedException("Cannot access another customer's appointments");
            }
            return ResponseEntity.ok(appointmentService.getAppointmentsByCustomer(customerId));
        }

        if (salonOwnerId != null) {
            if (!"SALON_OWNER".equals(userRole) || !authenticatedUserId.equals(salonOwnerId)) {
                throw new UnauthorizedException("Cannot access another salon owner's appointments");
            }
            return ResponseEntity.ok(appointmentService.getAppointmentsBySalonOwner(salonOwnerId));
        }

        throw new IllegalArgumentException("Must specify either customerId or salonOwnerId");
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status", description = "Update the status of an appointment with transition validation")
    public ResponseEntity<AppointmentResponse> updateAppointmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        
        log.info("Updating appointment {} status to: {}", id, request.getStatus());
        
        AppointmentResponse response = appointmentService.updateAppointmentStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel appointment")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long id) {
        log.info("Cancelling appointment: {}", id);
        AppointmentResponse response = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark appointment as completed — called by customer after service is done")
    public ResponseEntity<AppointmentResponse> completeAppointment(@PathVariable Long id) {
        log.info("Completing appointment: {}", id);
        UpdateAppointmentStatusRequest req = new UpdateAppointmentStatusRequest();
        req.setStatus(com.salon.entity.AppointmentStatus.COMPLETED);
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, req));
    }
}