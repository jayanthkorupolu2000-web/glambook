package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after fetching or editing a Salon Owner.
 * Contains all fields so the frontend can refresh the table row in one call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonOwnerManagementResponse {
    private Long id;
    private String ownerName;
    private String salonName;
    private String city;
    private String email;
    private String phone;
}
