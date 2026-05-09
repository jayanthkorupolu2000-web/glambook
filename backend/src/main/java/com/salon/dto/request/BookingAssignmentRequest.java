package com.salon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingAssignmentRequest {

    @NotNull(message = "Please provide a valid professionalId")
    private Long professionalId;

    private Long resourceId;
    private String notes;
}
