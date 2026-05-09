package com.salon.dto.request;

import lombok.Data;

@Data
public class BeautyProfileRequest {
    private String skinType;
    private String hairType;
    private String hairTexture;
    private String allergies;
    private String preferredServices;
    private String notes;
}
