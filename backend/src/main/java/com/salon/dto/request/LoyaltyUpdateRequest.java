package com.salon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoyaltyUpdateRequest {

    @NotNull(message = "Please provide a valid points")
    @Min(value = 0, message = "Points must be 0 or more")
    private Integer points;
}
