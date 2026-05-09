package com.salon.controller;

import com.salon.dto.response.ProductResponse;
import com.salon.dto.response.ServiceResponse;
import com.salon.service.FavoritesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Customer favorite products and services")
public class FavoritesController {

    private final FavoritesService favoritesService;

    // ─── Products ────────────────────────────────────────────────────────────

    @GetMapping("/products")
    @Operation(summary = "Get customer's favorite products")
    public ResponseEntity<List<ProductResponse>> getFavoriteProducts(@PathVariable Long customerId) {
        return ResponseEntity.ok(favoritesService.getFavoriteProducts(customerId));
    }

    @PostMapping("/products/{productId}")
    @Operation(summary = "Toggle favorite on a product (add if not present, remove if present)")
    public ResponseEntity<Map<String, Object>> toggleFavoriteProduct(
            @PathVariable Long customerId,
            @PathVariable Long productId) {
        favoritesService.toggleFavoriteProduct(customerId, productId);
        boolean now = favoritesService.isProductFavorited(customerId, productId);
        return ResponseEntity.ok(Map.of("favorited", now));
    }

    // ─── Services ────────────────────────────────────────────────────────────

    @GetMapping("/services")
    @Operation(summary = "Get customer's favorite services")
    public ResponseEntity<List<ServiceResponse>> getFavoriteServices(@PathVariable Long customerId) {
        return ResponseEntity.ok(favoritesService.getFavoriteServices(customerId));
    }

    @PostMapping("/services/{serviceId}")
    @Operation(summary = "Toggle favorite on a service (add if not present, remove if present)")
    public ResponseEntity<Map<String, Object>> toggleFavoriteService(
            @PathVariable Long customerId,
            @PathVariable Long serviceId) {
        favoritesService.toggleFavoriteService(customerId, serviceId);
        boolean now = favoritesService.isServiceFavorited(customerId, serviceId);
        return ResponseEntity.ok(Map.of("favorited", now));
    }
}
