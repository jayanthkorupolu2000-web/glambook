package com.salon.controller;

import com.salon.dto.response.ScheduleSlotResponse;
import com.salon.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Provides daily schedule and slot-status endpoints.
 * All slots returned include a computed slotStatus (AVAILABLE / UNAVAILABLE / COMPLETED)
 * so the frontend can render strike-through / disabled states without any client-side logic.
 */
@RestController
@RequestMapping("/api/v1/professionals/{professionalId}/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "Daily schedule and slot availability for professionals")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * GET /api/v1/professionals/{professionalId}/schedule/today
     *
     * Returns today's schedule excluding already-ended slots.
     * Past slots (endTime <= now) are filtered out entirely.
     * In-progress or future slots are returned with their computed status.
     */
    @GetMapping("/today")
    @Operation(
        summary = "Get today's schedule",
        description = "Returns today's slots for a professional. " +
                      "Slots whose end time has already passed are excluded. " +
                      "Remaining slots carry slotStatus=AVAILABLE or UNAVAILABLE."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schedule returned"),
        @ApiResponse(responseCode = "404", description = "Professional not found")
    })
    public ResponseEntity<List<ScheduleSlotResponse>> getTodaySchedule(
            @Parameter(description = "Professional ID") @PathVariable Long professionalId) {
        return ResponseEntity.ok(scheduleService.getTodaySchedule(professionalId));
    }

    /**
     * GET /api/v1/professionals/{professionalId}/schedule/{date}/slots
     *
     * Returns all slots for a given date with their computed status.
     * - Past date  → all UNAVAILABLE
     * - Today      → past slots UNAVAILABLE, future slots AVAILABLE
     * - Future     → all WORKING slots AVAILABLE
     */
    @GetMapping("/{date}/slots")
    @Operation(
        summary = "Get slots for a specific date",
        description = "Returns all availability slots for the given date. " +
                      "Each slot includes slotStatus so the UI can show " +
                      "green (AVAILABLE), red/strikethrough (UNAVAILABLE), or gray (COMPLETED)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Slots returned"),
        @ApiResponse(responseCode = "400", description = "Invalid date format")
    })
    public ResponseEntity<List<ScheduleSlotResponse>> getSlotsByDate(
            @Parameter(description = "Professional ID") @PathVariable Long professionalId,
            @Parameter(description = "Date in yyyy-MM-dd format") @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleService.getSlotsByDate(professionalId, date));
    }
}
