package com.salon.service;

import com.salon.dto.request.ProductOrderRequest;
import com.salon.dto.request.ProductReviewRequest;
import com.salon.dto.response.FavoriteResponseDTO;
import com.salon.dto.response.ProductOrderResponseDTO;
import com.salon.dto.response.ProductResponseDTO;
import com.salon.dto.response.ProductReviewResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    Page<ProductResponseDTO> getAllProducts(String category, BigDecimal minPrice,
                                           BigDecimal maxPrice, String brand,
                                           Long customerId, Pageable pageable);

    ProductResponseDTO getProductById(Long productId, Long customerId);

    List<ProductResponseDTO> searchProducts(String keyword, Long customerId);

    List<ProductResponseDTO> getRecommendedProducts(Long customerId);

    ProductOrderResponseDTO placeOrder(Long customerId, ProductOrderRequest request);

    List<ProductOrderResponseDTO> getOrderHistory(Long customerId);

    ProductOrderResponseDTO getOrderById(Long customerId, Long orderId);

    /** Process payment for a product order and mark it DELIVERED */
    ProductOrderResponseDTO payOrder(Long customerId, Long orderId, String method);

    FavoriteResponseDTO addToFavorites(Long customerId, Long productId);

    void removeFromFavorites(Long customerId, Long productId);

    List<FavoriteResponseDTO> getFavorites(Long customerId);

    ProductReviewResponseDTO addReview(Long customerId, Long productId, ProductReviewRequest request);

    List<ProductReviewResponseDTO> getReviews(Long productId);

    boolean hasDeliveredOrder(Long customerId, Long productId);

    boolean hasReviewed(Long customerId, Long productId);
}
