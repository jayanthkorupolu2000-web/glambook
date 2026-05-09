package com.salon.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProfessionalProfileUpdateRequest {

    @NotBlank(message = "Please provide a valid specialization")
    private String specialization;

    @NotNull(message = "Please provide a valid experienceYears")
    @Min(value = 0, message = "Experience years must be 0 or more")
    private Integer experienceYears;

    private String certifications;
    private String trainingDetails;
    private String serviceAreas;

    @Min(value = 0, message = "Travel radius must be 0 or more")
    private Integer travelRadiusKm;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    private String instagramHandle;
    private Boolean isAvailableHome;
    private Boolean isAvailableSalon;

    @Min(value = 1, message = "Response time must be at least 1 hour")
    private Integer responseTimeHrs;
}
