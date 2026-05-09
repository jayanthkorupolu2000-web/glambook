package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for editing a Salon Owner's name, salon name, and phone.
 * Email and city are intentionally excluded — they are read-only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonOwnerManagementEditRequest {

    @NotBlank(message = "Please provide a valid ownerName")
    private String ownerName;

    @NotBlank(message = "Please provide a valid salonName")
    private String salonName;

    @NotBlank(message = "Please provide a valid phone")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please provide a valid phone")
    private String phone;
}
