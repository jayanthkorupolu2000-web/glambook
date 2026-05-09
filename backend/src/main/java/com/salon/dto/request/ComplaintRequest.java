package com.salon.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ComplaintRequest {

    @NotNull(message = "Please provide a valid customerId")
    private Long customerId;

    @NotNull(message = "Please provide a valid professionalId")
    private Long professionalId;

    @NotBlank(message = "Please provide a valid description")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Please provide a valid feedback")
    @Pattern(regexp = "POOR|AVERAGE|GOOD|BETTER", message = "Feedback must be POOR, AVERAGE, GOOD, or BETTER")
    private String feedback;

    @NotNull(message = "Please provide a valid rating")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
}
