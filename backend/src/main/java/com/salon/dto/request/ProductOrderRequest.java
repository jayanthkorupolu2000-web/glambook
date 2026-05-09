package com.salon.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrderRequest {

    @NotNull(message = "Please provide a valid productId")
    private Long productId;

    @Min(value = 1, message = "Please provide a valid quantity")
    @Max(value = 10, message = "Please provide a valid quantity")
    private int quantity;
}
