package com.salon.dto.response;

import lombok.Data;

@Data
public class BeautyProfileResponse {
    private Long id;
    private Long customerId;
    private String skinType;
    private String hairType;
    private String hairTexture;
    private String allergies;
    private String preferredServices;
    private String notes;
}
