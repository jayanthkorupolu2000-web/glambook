package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after editing a Salon Owner.
 * Includes all fields — editable and read-only — for the frontend to refresh the table row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonOwnerEditResponse {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String city;
    private String role;
    private String additionalInfo; // maps to salonName
}
