package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for editing a Salon Owner's name, phone and salon name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonOwnerEditRequest {

    @NotBlank(message = "Please provide a valid name")
    private String name;

    @NotBlank(message = "Please provide a valid phone")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please provide a valid phone")
    private String phone;

    @NotBlank(message = "Please provide a salon name")
    private String salonName;
}
