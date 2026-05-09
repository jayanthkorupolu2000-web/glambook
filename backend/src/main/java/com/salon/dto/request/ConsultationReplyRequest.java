package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationReplyRequest {
    @NotBlank(message = "Please provide a valid notes")
    @Size(min = 10, max = 2000, message = "Please provide a valid notes")
    private String notes;
}
