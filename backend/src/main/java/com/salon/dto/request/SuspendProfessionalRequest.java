package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendProfessionalRequest {

    @NotBlank(message = "Suspension reason is required")
    private String reason;

    /**
     * Number of days for the suspension.
     * null or 0 means permanent suspension.
     */
    private Integer durationDays;
}
