package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MediationActionRequest {

    @NotBlank(message = "Please provide valid ownerActionNotes")
    @Size(min = 10, max = 2000, message = "Notes must be between 10 and 2000 characters")
    private String ownerActionNotes;
}
