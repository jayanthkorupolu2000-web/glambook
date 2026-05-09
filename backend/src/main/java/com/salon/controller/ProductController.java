package com.salon.controller;

import com.salon.dto.request.ProductOrderRequest;
import com.salon.dto.request.ProductReviewRequest;
import com.salon.dto.response.FavoriteResponseDTO;
import com.salon.dto.response.ProductOrderResponseDTO;
import com.salon.dto.response.ProductResponseDTO;
import com.salon.dto.response.ProductReviewResponseDTO;
import com.salon.exception.UnauthorizedException;
import com.salon.security.JwtUtil;
import com.salon.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog, ordering, favorites and reviews")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;
    private final JwtUtil jwtUtil;

    // ── Public endpoints ──────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Browse all products with optional filters and pagination")
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            HttpServletRequest req) {

        // Validate price range
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new com.salon.exception.ValidationException(
                    "Please provide a valid minPrice — must be greater than or equal to 0");
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new com.salon.exception.ValidationException(
                    "Please provide a valid maxPrice — must be greater than or equal to 0");
        if (minPrice != null && maxPrice != null && maxPrice.compareTo(minPrice) < 0)
            throw new com.salon.exception.ValidationException(
                    "maxPrice must be greater than or equal to minPrice");

        Long customerId = extractCustomerIdOptional(req);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.ok(
                productService.getAllProducts(category, minPrice, maxPrice, brand, customerId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID")
    public ResponseEntity<ProductResponseDTO> getProductById(
            @PathVariable Long id, HttpServletRequest req) {
        Long customerId = extractCustomerIdOptional(req);
        return ResponseEntity.ok(productService.getProductById(id, customerId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by keyword (name, brand, description)")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(
            @RequestParam("q") String keyword, HttpServletRequest req) {
        Long customerId = extractCustomerIdOptional(req);
        return ResponseEntity.ok(productService.searchProducts(keyword, customerId));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get all reviews for a product")
    public ResponseEntity<List<ProductReviewResponseDTO>> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getReviews(id));
    }

    @GetMapping("/{id}/can-review")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Check if the logged-in customer can review this product (must have a DELIVERED order)")
    public ResponseEntity<java.util.Map<String, Object>> canReview(
            @PathVariable Long id, HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        boolean delivered = productService.hasDeliveredOrder(customerId, id);
        boolean alreadyReviewed = productService.hasReviewed(customerId, id);
        return ResponseEntity.ok(java.util.Map.of(
                "canReview", delivered && !alreadyReviewed,
                "hasDeliveredOrder", delivered,
                "alreadyReviewed", alreadyReviewed
        ));
    }

    // ── Auth-required endpoints ───────────────────────────────────────────────

    @GetMapping("/recommended")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get recommended products based on customer's appointment history")
    public ResponseEntity<List<ProductResponseDTO>> getRecommended(HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.ok(productService.getRecommendedProducts(customerId));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Place a product order")
    public ResponseEntity<ProductOrderResponseDTO> placeOrder(
            @Valid @RequestBody ProductOrderRequest request,
            HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.status(201).body(productService.placeOrder(customerId, request));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get order history for the logged-in customer")
    public ResponseEntity<List<ProductOrderResponseDTO>> getOrderHistory(HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.ok(productService.getOrderHistory(customerId));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get a specific order by ID")
    public ResponseEntity<ProductOrderResponseDTO> getOrderById(
            @PathVariable Long orderId, HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.ok(productService.getOrderById(customerId, orderId));
    }

    @PostMapping("/orders/{orderId}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Pay for a product order (marks it as DELIVERED and awards loyalty points)")
    public ResponseEntity<ProductOrderResponseDTO> payOrder(
            @PathVariable Long orderId,
            @RequestBody java.util.Map<String, Object> body,
            HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        String method = (String) body.getOrDefault("method", "CASH");
        java.math.BigDecimal walletAmountUsed = java.math.BigDecimal.ZERO;
        Object w = body.get("walletAmountUsed");
        if (w != null) {
            try { walletAmountUsed = new java.math.BigDecimal(w.toString()); } catch (Exception ignored) {}
        }
        com.salon.service.impl.ProductServiceImpl impl =
                (com.salon.service.impl.ProductServiceImpl) productService;
        return ResponseEntity.ok(impl.payOrder(customerId, orderId, method, walletAmountUsed));
    }

    @PostMapping("/{id}/favorites")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add product to favorites")
    public ResponseEntity<FavoriteResponseDTO> addToFavorites(
            @PathVariable Long id, HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.status(201).body(productService.addToFavorites(customerId, id));
    }

    @DeleteMapping("/{id}/favorites")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove product from favorites")
    public ResponseEntity<Void> removeFromFavorites(
            @PathVariable Long id, HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        productService.removeFromFavorites(customerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get all favorited products for the logged-in customer")
    public ResponseEntity<List<FavoriteResponseDTO>> getFavorites(HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.ok(productService.getFavorites(customerId));
    }

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add a review for a product (must have ordered it)")
    public ResponseEntity<ProductReviewResponseDTO> addReview(
            @PathVariable Long id,
            @Valid @RequestBody ProductReviewRequest request,
            HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.status(201).body(productService.addReview(customerId, id, request));
    }

    @PatchMapping("/{id}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update your review for a product")
    public ResponseEntity<ProductReviewResponseDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ProductReviewRequest request,
            HttpServletRequest req) {
        Long customerId = extractCustomerId(req);
        return ResponseEntity.ok(productService.updateReview(customerId, id, request));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long extractCustomerId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new UnauthorizedException("Missing or invalid authorization header");
        return jwtUtil.extractUserId(auth.substring(7));
    }

    private Long extractCustomerIdOptional(HttpServletRequest req) {
        try {
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer "))
                return jwtUtil.extractUserId(auth.substring(7));
        } catch (Exception ignored) {}
        return null;
    }
}
