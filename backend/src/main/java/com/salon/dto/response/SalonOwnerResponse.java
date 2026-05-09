package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonOwnerResponse {
    private Long id;
    private String name;
    private String salonName;
    private String city;
    private String email;
    private String phone;
}
