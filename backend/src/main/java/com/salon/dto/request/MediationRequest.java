package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MediationRequest {

    @NotBlank(message = "Please provide resolution notes")
    @Size(max = 2000, message = "Resolution notes must not exceed 2000 characters")
    private String resolutionNotes;
}
