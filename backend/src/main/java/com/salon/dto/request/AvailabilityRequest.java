package com.salon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AvailabilityRequest {

    @NotNull(message = "Please provide a valid availDate")
    private LocalDate availDate;

    @NotNull(message = "Please provide a valid startTime")
    private LocalTime startTime;

    @NotNull(message = "Please provide a valid endTime")
    private LocalTime endTime;

    private String slotType; // WORKING, LUNCH_BREAK, BREAK, BLOCKED — defaults to WORKING
}
