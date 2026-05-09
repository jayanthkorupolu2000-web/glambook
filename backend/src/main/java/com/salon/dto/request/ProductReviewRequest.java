package com.salon.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewRequest {

    @Min(value = 1, message = "Please provide a valid rating")
    @Max(value = 5, message = "Please provide a valid rating")
    private int rating;

    @NotBlank(message = "Please provide a valid reviewText")
    private String reviewText;
}
