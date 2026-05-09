package com.salon.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PromotionRequest {

    @NotNull(message = "Please provide a valid ownerId")
    private Long ownerId;

    @NotBlank(message = "Please provide a valid title")
    private String title;

    private String description;

    @NotNull(message = "Please provide a valid discountPct")
    @DecimalMin(value = "1.0", message = "Discount must be at least 1%")
    @DecimalMax(value = "100.0", message = "Discount must not exceed 100%")
    private BigDecimal discountPct;

    @NotNull(message = "Please provide a valid startDate")
    private LocalDate startDate;

    @NotNull(message = "Please provide a valid endDate")
    private LocalDate endDate;
}
