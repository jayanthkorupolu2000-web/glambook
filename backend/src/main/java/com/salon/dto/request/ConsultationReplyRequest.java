package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationReplyRequest {
    @NotBlank(message = "Please provide a reply message")
    @Size(min = 10, max = 2000, message = "Reply must be between 10 and 2000 characters")
    private String professionalReply;
}
