package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AvailabilityResponse {
    private Long id;
    private Long professionalId;
    private LocalDate availDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isBooked;
    private String slotType; // WORKING, LUNCH_BREAK, BREAK, BLOCKED
}
