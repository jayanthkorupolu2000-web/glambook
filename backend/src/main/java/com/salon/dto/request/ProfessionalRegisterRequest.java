package com.salon.dto.request;

import com.salon.validation.ValidCity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalRegisterRequest {

    @NotBlank(message = "Please provide a valid name")
    @Size(min = 2, message = "Please provide a valid name")
    private String name;

    @NotBlank(message = "Please provide a valid email")
    @Email(message = "Please provide a valid email")
    private String email;

    @NotBlank(message = "Please provide a valid password")
    @Size(min = 8, message = "Please provide a valid password")
    private String password;

    @NotBlank(message = "Please provide a valid city")
    @ValidCity
    private String city;

    @NotBlank(message = "Please provide a valid specialization")
    private String specialization;

    // Optional: link to a specific service from the salon owner's service list
    private Long serviceId;
}
