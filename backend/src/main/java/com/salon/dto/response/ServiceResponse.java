package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponse {
    private Long id;
    private String name;
    private String category;
    private String gender;
    private BigDecimal price;
    private Integer durationMins;
    private BigDecimal discountPct;
    private Boolean isActive;
    private boolean favorited;
    private Long professionalId;
}
