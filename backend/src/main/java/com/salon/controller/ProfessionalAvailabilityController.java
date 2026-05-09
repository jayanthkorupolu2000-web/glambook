package com.salon.controller;

import com.salon.dto.request.AvailabilityRequest;
import com.salon.dto.response.AvailabilityResponse;
import com.salon.entity.Professional;
import com.salon.entity.ProfessionalAvailability;
import com.salon.exception.ConflictException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.ProfessionalAvailabilityRepository;
import com.salon.repository.ProfessionalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/professionals/{professionalId}/availability")
@RequiredArgsConstructor
@Tag(name = "Professional Availability")
public class ProfessionalAvailabilityController {

    private final ProfessionalAvailabilityRepository availabilityRepository;
    private final ProfessionalRepository professionalRepository;
    private final com.salon.repository.AppointmentRepository appointmentRepository;

    @GetMapping
    @Operation(summary = "Get availability slots for a professional")
    public ResponseEntity<List<AvailabilityResponse>> getSlots(@PathVariable Long professionalId) {
        List<AvailabilityResponse> slots = availabilityRepository
                .findByProfessionalId(professionalId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/by-date")
    @Operation(summary = "Get availability slots for a professional on a specific date")
    public ResponseEntity<List<AvailabilityResponse>> getSlotsByDate(
            @PathVariable Long professionalId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AvailabilityResponse> slots = availabilityRepository
                .findByProfessionalIdAndAvailDate(professionalId, date)
                .stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/time-slots")
    @Operation(summary = "Generate bookable time slots for a date based on service duration")
    public ResponseEntity<List<java.util.Map<String, Object>>> getTimeSlots(
            @PathVariable Long professionalId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "30") int durationMins) {

        // Get all availability windows for this date
        List<ProfessionalAvailability> windows = availabilityRepository
                .findByProfessionalIdAndAvailDate(professionalId, date)
                .stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        // Get all booked appointments for this professional on this date
        List<com.salon.entity.Appointment> bookedAppts = appointmentRepository
                .findByProfessionalIdAndDateTimeBetween(
                        professionalId,
                        date.atStartOfDay(),
                        date.atTime(23, 59, 59));

        // Build list of booked time ranges [start, end)
        java.util.List<java.time.LocalTime[]> bookedRanges = bookedAppts.stream()
                .filter(a -> a.getStatus() == com.salon.entity.AppointmentStatus.PENDING
                          || a.getStatus() == com.salon.entity.AppointmentStatus.CONFIRMED)
                .map(a -> {
                    java.time.LocalTime s = a.getDateTime().toLocalTime();
                    int dur = a.getService() != null && a.getService().getDurationMins() != null
                            ? a.getService().getDurationMins() : durationMins;
                    return new java.time.LocalTime[]{ s, s.plusMinutes(dur) };
                })
                .collect(Collectors.toList());

        // Completed appointment times
        java.util.Set<java.time.LocalTime> completedTimes = bookedAppts.stream()
                .filter(a -> a.getStatus() == com.salon.entity.AppointmentStatus.COMPLETED)
                .map(a -> a.getDateTime().toLocalTime())
                .collect(java.util.stream.Collectors.toSet());

        // Current time for today's past-slot detection
        boolean isToday = date.equals(java.time.LocalDate.now());
        java.time.LocalTime now = isToday ? java.time.LocalTime.now() : null;

        // Generate slots from WORKING windows only
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        for (ProfessionalAvailability window : windows) {
            if (window.getSlotType() != com.salon.entity.SlotType.WORKING) continue;

            java.time.LocalTime cursor = window.getStartTime();
            java.time.LocalTime windowEnd = window.getEndTime();

            while (!cursor.plusMinutes(durationMins).isAfter(windowEnd)) {
                java.time.LocalTime slotEnd = cursor.plusMinutes(durationMins);
                final java.time.LocalTime slotStart = cursor;

                // Check if this slot overlaps any booked appointment
                boolean booked = bookedRanges.stream().anyMatch(r ->
                        slotStart.isBefore(r[1]) && slotEnd.isAfter(r[0]));

                // Check if this slot overlaps any break/lunch window
                boolean overlapsBreak = windows.stream()
                        .filter(w -> w.getSlotType() != com.salon.entity.SlotType.WORKING)
                        .anyMatch(w -> slotStart.isBefore(w.getEndTime()) && slotEnd.isAfter(w.getStartTime()));

                // Past slot detection (today only)
                boolean past = now != null && !slotEnd.isAfter(now);

                // Completed slot
                boolean completed = completedTimes.contains(slotStart);

                // Compute slotStatus
                String slotStatus;
                if (completed) {
                    slotStatus = "COMPLETED";
                } else if (past || booked || overlapsBreak) {
                    slotStatus = "UNAVAILABLE";
                } else {
                    slotStatus = "AVAILABLE";
                }

                java.util.Map<String, Object> slot = new java.util.LinkedHashMap<>();
                slot.put("startTime", cursor.toString());
                slot.put("endTime", slotEnd.toString());
                slot.put("booked", booked || overlapsBreak || completed);
                slot.put("slotType", "WORKING");
                slot.put("slotStatus", slotStatus);
                slot.put("past", past);
                result.add(slot);

                cursor = cursor.plusMinutes(durationMins);
            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Add an availability slot")
    public ResponseEntity<AvailabilityResponse> addSlot(
            @PathVariable Long professionalId,
            @Valid @RequestBody AvailabilityRequest req) {

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        // Check for duplicate slot
        boolean exists = availabilityRepository
                .findByProfessionalIdAndAvailDateAndStartTime(professionalId, req.getAvailDate(), req.getStartTime())
                .isPresent();
        if (exists) {
            throw new ConflictException("A slot already exists for this date and time");
        }

        ProfessionalAvailability slot = ProfessionalAvailability.builder()
                .professional(professional)
                .availDate(req.getAvailDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .isBooked(false)
                .slotType(req.getSlotType() != null
                        ? com.salon.entity.SlotType.valueOf(req.getSlotType())
                        : com.salon.entity.SlotType.WORKING)
                .build();

        ProfessionalAvailability saved = availabilityRepository.save(slot);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @DeleteMapping("/{slotId}")
    @Operation(summary = "Delete an availability slot")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable Long professionalId,
            @PathVariable Long slotId) {
        ProfessionalAvailability slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        if (slot.isBooked()) {
            throw new ConflictException("Cannot delete a booked slot");
        }
        availabilityRepository.delete(slot);
        return ResponseEntity.noContent().build();
    }

    private AvailabilityResponse toResponse(ProfessionalAvailability s) {
        AvailabilityResponse r = new AvailabilityResponse();
        r.setId(s.getId());
        r.setProfessionalId(s.getProfessional().getId());
        r.setAvailDate(s.getAvailDate());
        r.setStartTime(s.getStartTime());
        r.setEndTime(s.getEndTime());
        r.setBooked(s.isBooked());
        r.setSlotType(s.getSlotType() != null ? s.getSlotType().name() : "WORKING");
        return r;
    }
}
