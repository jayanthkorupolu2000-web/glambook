package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortfolioItemRequest {

    @NotNull(message = "Please provide a valid professionalId")
    private Long professionalId;

    private Long serviceId;

    @NotBlank(message = "Please provide a valid mediaType")
    private String mediaType;

    @NotBlank(message = "Please provide a valid serviceTag")
    private String serviceTag;

    private String caption;
    private String testimonial;
    private Boolean isFeatured;
}
