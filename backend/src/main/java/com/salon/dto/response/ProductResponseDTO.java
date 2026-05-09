package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String brand;
    private BigDecimal price;
    private int stockQuantity;
    private String imageUrl;
    private String ingredients;
    private String usageTips;
    private double averageRating;
    private long reviewCount;
    private boolean favorited;
}
