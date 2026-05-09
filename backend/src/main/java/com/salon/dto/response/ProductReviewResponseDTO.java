package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewResponseDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private int rating;
    private String reviewText;
    private LocalDateTime createdAt;
}
