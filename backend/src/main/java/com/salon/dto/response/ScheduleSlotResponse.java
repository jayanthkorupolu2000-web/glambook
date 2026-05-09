package com.salon.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a single time slot in the daily schedule,
 * enriched with its computed SlotStatus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A time slot in the professional's daily schedule")
public class ScheduleSlotResponse {

    @Schema(description = "Slot DB id (0 for generated slots not yet persisted)")
    private Long id;

    @Schema(description = "Date of the slot")
    private LocalDate date;

    @Schema(description = "Slot start time (HH:mm)")
    private LocalTime startTime;

    @Schema(description = "Slot end time (HH:mm)")
    private LocalTime endTime;

    @Schema(description = "Slot type: WORKING, LUNCH_BREAK, BREAK, BLOCKED")
    private String slotType;

    @Schema(description = "Computed status: AVAILABLE, UNAVAILABLE, COMPLETED")
    private String slotStatus;

    @Schema(description = "True if an appointment is booked in this slot")
    private boolean booked;

    @Schema(description = "True if this slot is in the past or currently in-progress")
    private boolean past;
}
