package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SalonPolicyRequest {

    // Set by the controller from the path variable — not validated in the request body
    private Long ownerId;

    @NotBlank(message = "Please provide a valid title")
    private String title;

    @NotBlank(message = "Please provide a valid content")
    @Size(min = 20, message = "Content must be at least 20 characters")
    private String content;
}
