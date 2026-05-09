package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLoginRequest {

    @NotBlank(message = "Please provide a valid username")
    private String username;

    @NotBlank(message = "Please provide a valid password")
    private String password;
}
