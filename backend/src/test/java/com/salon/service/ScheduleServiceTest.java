package com.salon.service;

import com.salon.dto.response.ScheduleSlotResponse;
import com.salon.entity.*;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.ProfessionalAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ScheduleService.
 * Uses Mockito to mock repositories so no DB is needed.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock ProfessionalAvailabilityRepository availabilityRepository;
    @Mock AppointmentRepository appointmentRepository;

    @InjectMocks ScheduleService scheduleService;

    private static final Long PROF_ID = 1L;
    private static final LocalDate TODAY = LocalDate.now();

    // Helper: build a WORKING availability window
    private ProfessionalAvailability window(LocalTime start, LocalTime end) {
        Professional prof = new Professional();
        prof.setId(PROF_ID);
        return ProfessionalAvailability.builder()
                .id(1L)
                .professional(prof)
                .availDate(TODAY)
                .startTime(start)
                .endTime(end)
                .slotType(SlotType.WORKING)
                .slotStatus(SlotStatus.AVAILABLE)
                .build();
    }

    @BeforeEach
    void setUp() {
        // Default: no appointments
        when(appointmentRepository.findByProfessionalIdAndDateTimeBetween(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
    }

    // ── generateDailySchedule_excludesPastSlots ───────────────────────────────

    @Test
    @DisplayName("getTodaySchedule: slots whose endTime is in the past are excluded")
    void getTodaySchedule_excludesPastSlots() {
        // Arrange: one slot that ended 2 hours ago, one that ends in 2 hours
        LocalTime pastEnd   = LocalTime.now().minusHours(2);
        LocalTime futureEnd = LocalTime.now().plusHours(2);

        ProfessionalAvailability pastWindow   = window(pastEnd.minusHours(1), pastEnd);
        ProfessionalAvailability futureWindow = window(LocalTime.now().plusHours(1), futureEnd);

        when(availabilityRepository.findByProfessionalIdAndAvailDate(PROF_ID, TODAY))
                .thenReturn(List.of(pastWindow, futureWindow));

        // Act
        List<ScheduleSlotResponse> result = scheduleService.getTodaySchedule(PROF_ID);

        // Assert: only the future slot is returned
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSlotStatus()).isEqualTo("AVAILABLE");
        assertThat(result.get(0).isPast()).isFalse();
    }

    @Test
    @DisplayName("getTodaySchedule: in-progress slot (started but not ended) is UNAVAILABLE")
    void getTodaySchedule_inProgressSlot_isUnavailable() {
        // Arrange: slot that started 10 min ago and ends in 20 min
        LocalTime start = LocalTime.now().minusMinutes(10);
        LocalTime end   = LocalTime.now().plusMinutes(20);

        when(availabilityRepository.findByProfessionalIdAndAvailDate(PROF_ID, TODAY))
                .thenReturn(List.of(window(start, end)));

        // Act
        List<ScheduleSlotResponse> result = scheduleService.getTodaySchedule(PROF_ID);

        // Assert: slot is returned (not excluded) but marked UNAVAILABLE
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSlotStatus()).isEqualTo("UNAVAILABLE");
    }

    // ── getAvailableSlots_marksUnavailableCorrectly ───────────────────────────

    @Test
    @DisplayName("getSlotsByDate today: future slot is AVAILABLE, past slot is UNAVAILABLE")
    void getSlotsByDate_today_correctStatuses() {
        LocalTime pastStart   = LocalTime.now().minusHours(3);
        LocalTime pastEnd     = LocalTime.now().minusHours(2);
        LocalTime futureStart = LocalTime.now().plusHours(1);
        LocalTime futureEnd   = LocalTime.now().plusHours(2);

        when(availabilityRepository.findByProfessionalIdAndAvailDate(PROF_ID, TODAY))
                .thenReturn(List.of(
                        window(pastStart, pastEnd),
                        window(futureStart, futureEnd)
                ));

        List<ScheduleSlotResponse> result = scheduleService.getSlotsByDate(PROF_ID, TODAY);

        assertThat(result).hasSize(2);

        ScheduleSlotResponse past   = result.stream().filter(s -> s.getStartTime().equals(pastStart)).findFirst().orElseThrow();
        ScheduleSlotResponse future = result.stream().filter(s -> s.getStartTime().equals(futureStart)).findFirst().orElseThrow();

        assertThat(past.getSlotStatus()).isEqualTo("UNAVAILABLE");
        assertThat(past.isPast()).isTrue();

        assertThat(future.getSlotStatus()).isEqualTo("AVAILABLE");
        assertThat(future.isPast()).isFalse();
    }

    @Test
    @DisplayName("getSlotsByDate future date: all WORKING slots are AVAILABLE")
    void getSlotsByDate_futureDate_allAvailable() {
        LocalDate tomorrow = TODAY.plusDays(1);
        when(availabilityRepository.findByProfessionalIdAndAvailDate(PROF_ID, tomorrow))
                .thenReturn(List.of(
                        window(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        window(LocalTime.of(10, 0), LocalTime.of(11, 0))
                ));

        List<ScheduleSlotResponse> result = scheduleService.getSlotsByDate(PROF_ID, tomorrow);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> "AVAILABLE".equals(s.getSlotStatus()));
        assertThat(result).allMatch(s -> !s.isPast());
    }

    @Test
    @DisplayName("getSlotsByDate past date: all slots are UNAVAILABLE")
    void getSlotsByDate_pastDate_allUnavailable() {
        LocalDate yesterday = TODAY.minusDays(1);
        when(availabilityRepository.findByProfessionalIdAndAvailDate(PROF_ID, yesterday))
                .thenReturn(List.of(
                        window(LocalTime.of(9, 0), LocalTime.of(10, 0))
                ));

        List<ScheduleSlotResponse> result = scheduleService.getSlotsByDate(PROF_ID, yesterday);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSlotStatus()).isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("Booked slot is UNAVAILABLE regardless of time")
    void bookedSlot_isUnavailable() {
        LocalTime start = LocalTime.now().plusHours(1);
        LocalTime end   = LocalTime.now().plusHours(2);

        when(availabilityRepository.findByProfessionalIdAndAvailDate(PROF_ID, TODAY))
                .thenReturn(List.of(window(start, end)));

        // Simulate a booked appointment at this time
        Appointment appt = new Appointment();
        appt.setStatus(AppointmentStatus.CONFIRMED);
        appt.setDateTime(TODAY.atTime(start));
        when(appointmentRepository.findByProfessionalIdAndDateTimeBetween(
                anyLong(), any(), any()))
                .thenReturn(List.of(appt));

        List<ScheduleSlotResponse> result = scheduleService.getSlotsByDate(PROF_ID, TODAY);

        assertThat(result.get(0).getSlotStatus()).isEqualTo("UNAVAILABLE");
        assertThat(result.get(0).isBooked()).isTrue();
    }

    @Test
    @DisplayName("Completed appointment slot is COMPLETED")
    void completedAppointment_slotIsCompleted() {
        LocalTime start = LocalTime.now().minusHours(2);
        LocalTime end   = LocalTime.now().minusHours(1);

        when(availabilityRepository.findByProfessionalIdAndAvailDate(PROF_ID, TODAY))
                .thenReturn(List.of(window(start, end)));

        Appointment appt = new Appointment();
        appt.setStatus(AppointmentStatus.COMPLETED);
        appt.setDateTime(TODAY.atTime(start));
        when(appointmentRepository.findByProfessionalIdAndDateTimeBetween(
                anyLong(), any(), any()))
                .thenReturn(List.of(appt));

        List<ScheduleSlotResponse> result = scheduleService.buildSchedule(PROF_ID, TODAY, LocalTime.now());

        assertThat(result.get(0).getSlotStatus()).isEqualTo("COMPLETED");
    }
}
