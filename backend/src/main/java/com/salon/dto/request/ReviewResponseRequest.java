package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewResponseRequest {

    @NotBlank(message = "Please provide a valid response")
    @Size(max = 1000, message = "Response must not exceed 1000 characters")
    private String response;
}
