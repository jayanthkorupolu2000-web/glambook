package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ResourceAvailabilityResponse {
    private Long id;
    private Long resourceId;
    private LocalDate availDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isBooked;
}
