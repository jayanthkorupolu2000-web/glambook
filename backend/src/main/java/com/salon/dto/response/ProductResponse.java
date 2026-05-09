package com.salon.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String brand;
    private String category;
    private String description;
    private String ingredients;
    private String usageTips;
    private BigDecimal price;
    private int stock;
    private String imageUrl;
    private String recommendedFor;
    private boolean favorited;
}
