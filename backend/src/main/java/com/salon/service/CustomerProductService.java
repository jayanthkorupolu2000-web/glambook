package com.salon.service;

import com.salon.dto.response.ProductResponse;

import java.util.List;

public interface CustomerProductService {
    List<ProductResponse> browseProducts(String category, String brand, String keyword);
    ProductResponse getProductById(Long productId);
}
