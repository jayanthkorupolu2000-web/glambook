package com.salon.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PromotionResponse {
    private Long id;
    private Long ownerId;
    private String title;
    private String description;
    private BigDecimal discountPct;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
}
