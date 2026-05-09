package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommunicationRequest {

    @NotNull(message = "Please provide a valid customerId")
    private Long customerId;

    private Long appointmentId;

    @NotBlank(message = "Please provide a valid message")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;

    @NotBlank(message = "Please provide a valid type")
    private String type;
}
