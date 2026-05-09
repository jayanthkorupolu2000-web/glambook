package com.salon.controller;

import com.salon.dto.response.ProductResponse;
import com.salon.service.CustomerProductService;
import com.salon.service.FavoritesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog for customers")
public class CustomerProductController {

    private final CustomerProductService productService;
    private final FavoritesService favoritesService;

    @GetMapping("/api/v1/products")
    @Operation(summary = "Browse products")
    public ResponseEntity<List<ProductResponse>> browse(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long customerId) {
        List<ProductResponse> products = productService.browseProducts(category, brand, keyword);
        return ResponseEntity.ok(favoritesService.enrichProducts(products, customerId));
    }

    @GetMapping("/api/v1/products/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }
}
