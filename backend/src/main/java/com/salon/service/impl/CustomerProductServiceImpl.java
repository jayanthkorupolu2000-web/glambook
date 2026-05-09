package com.salon.service.impl;

import com.salon.dto.response.ProductResponse;
import com.salon.entity.Product;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.ProductRepository;
import com.salon.service.CustomerProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerProductServiceImpl implements CustomerProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponse> browseProducts(String category, String brand, String keyword) {
        List<Product> products;
        if (keyword != null && !keyword.isBlank()) {
            products = productRepository.searchProducts(keyword);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByCategoryAndIsActiveTrue(category);
        } else if (brand != null && !brand.isBlank()) {
            products = productRepository.findByBrandContainingIgnoreCaseAndIsActiveTrue(brand);
        } else {
            products = productRepository.findByIsActiveTrue();
        }
        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public ProductResponse getProductById(Long productId) {
        return productRepository.findById(productId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private ProductResponse toResponse(Product p) {
        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setBrand(p.getBrand());
        res.setCategory(p.getCategory());
        res.setDescription(p.getDescription());
        res.setIngredients(p.getIngredients());
        res.setUsageTips(p.getUsageTips());
        res.setPrice(p.getPrice());
        res.setStock(p.getStock());
        res.setImageUrl(p.getImageUrl());
        res.setRecommendedFor(p.getRecommendedFor());
        return res;
    }
}
