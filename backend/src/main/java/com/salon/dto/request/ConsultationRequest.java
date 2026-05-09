package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationRequest {
    @NotNull(message = "Please provide a valid customerId")
    private Long customerId;

    private Long professionalId;
    private Long appointmentId;

    private String topic = "GENERAL";

    @NotBlank(message = "Please provide a valid question")
    @Size(min = 3, max = 2000, message = "Please provide a valid question")
    private String question;
}
