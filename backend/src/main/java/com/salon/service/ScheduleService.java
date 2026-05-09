package com.salon.service;

import com.salon.dto.response.ScheduleSlotResponse;
import com.salon.entity.*;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.ProfessionalAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the daily schedule for a professional and computes
 * the SlotStatus of every slot based on the current time.
 *
 * Rules:
 *  - Today, slots whose endTime <= now  → UNAVAILABLE (past)
 *  - Today, slot whose startTime <= now < endTime → UNAVAILABLE (in-progress)
 *  - Today, slots whose startTime > now → AVAILABLE (unless booked/break)
 *  - Future dates → all WORKING slots are AVAILABLE
 *  - Past dates   → all slots are UNAVAILABLE
 *  - Booked slots → UNAVAILABLE (appointment exists)
 *  - Completed appointments → COMPLETED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final ProfessionalAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns today's schedule for a professional.
     * Past slots are excluded from the result (they are not shown).
     */
    public List<ScheduleSlotResponse> getTodaySchedule(Long professionalId) {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        List<ScheduleSlotResponse> all = buildSchedule(professionalId, today, now);

        // Exclude slots that have already ended — they are not useful to show
        return all.stream()
                .filter(s -> !s.isPast() || SlotStatus.COMPLETED.name().equals(s.getSlotStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Returns all slots for a given date with their computed status.
     * For today: past slots are marked UNAVAILABLE.
     * For future dates: all WORKING slots are AVAILABLE.
     * For past dates: all slots are UNAVAILABLE.
     */
    public List<ScheduleSlotResponse> getSlotsByDate(Long professionalId, LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalTime now   = today.equals(date) ? LocalTime.now() : null;

        if (date.isBefore(today)) {
            // Past date — everything unavailable
            return buildSchedule(professionalId, date, LocalTime.MAX);
        }
        return buildSchedule(professionalId, date, now);
    }

    // ── Core builder ──────────────────────────────────────────────────────────

    /**
     * Builds the slot list for a date.
     * @param now null means future date (all WORKING slots → AVAILABLE)
     */
    List<ScheduleSlotResponse> buildSchedule(Long professionalId, LocalDate date, LocalTime now) {
        List<ProfessionalAvailability> windows = availabilityRepository
                .findByProfessionalIdAndAvailDate(professionalId, date)
                .stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        // Booked appointments for this date
        List<Appointment> appts = appointmentRepository
                .findByProfessionalIdAndDateTimeBetween(
                        professionalId,
                        date.atStartOfDay(),
                        date.atTime(23, 59, 59));

        List<ScheduleSlotResponse> result = new ArrayList<>();

        for (ProfessionalAvailability window : windows) {
            ScheduleSlotResponse slot = toSlotResponse(window, appts, now);
            result.add(slot);
        }

        return result;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private ScheduleSlotResponse toSlotResponse(
            ProfessionalAvailability window,
            List<Appointment> appts,
            LocalTime now) {

        LocalTime start = window.getStartTime();
        LocalTime end   = window.getEndTime();
        SlotType  type  = window.getSlotType() != null ? window.getSlotType() : SlotType.WORKING;

        // Determine if a completed appointment occupies this slot
        boolean completed = appts.stream().anyMatch(a ->
                a.getStatus() == AppointmentStatus.COMPLETED
                && a.getDateTime().toLocalTime().equals(start));

        // Determine if an active appointment occupies this slot
        boolean booked = appts.stream().anyMatch(a ->
                (a.getStatus() == AppointmentStatus.PENDING
                 || a.getStatus() == AppointmentStatus.CONFIRMED)
                && a.getDateTime().toLocalTime().equals(start));

        // Compute past flag
        boolean past = false;
        if (now != null) {
            // Slot is past if its end time is <= now, OR it started but hasn't ended (in-progress)
            past = !end.isAfter(now);
        }

        // Compute SlotStatus
        SlotStatus status;
        if (completed) {
            status = SlotStatus.COMPLETED;
        } else if (type != SlotType.WORKING) {
            // Break/lunch/blocked slots are never available for booking
            status = SlotStatus.UNAVAILABLE;
        } else if (past) {
            status = SlotStatus.UNAVAILABLE;
        } else if (booked) {
            status = SlotStatus.UNAVAILABLE;
        } else {
            status = SlotStatus.AVAILABLE;
        }

        return ScheduleSlotResponse.builder()
                .id(window.getId())
                .date(window.getAvailDate())
                .startTime(start)
                .endTime(end)
                .slotType(type.name())
                .slotStatus(status.name())
                .booked(booked || completed)
                .past(past)
                .build();
    }
}
