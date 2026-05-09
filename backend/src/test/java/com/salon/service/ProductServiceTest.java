package com.salon.service;

import com.salon.dto.request.ProductOrderRequest;
import com.salon.dto.request.ProductReviewRequest;
import com.salon.dto.response.FavoriteResponseDTO;
import com.salon.dto.response.ProductOrderResponseDTO;
import com.salon.dto.response.ProductResponseDTO;
import com.salon.dto.response.ProductReviewResponseDTO;
import com.salon.entity.*;
import com.salon.exception.ConflictException;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import com.salon.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductOrderRepository orderRepository;
    @Mock private FavoriteProductRepository favoriteProductRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private LoyaltyService loyaltyService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice").email("alice@test.com").build();
        product = Product.builder()
                .id(10L).name("Argan Oil Shampoo").brand("L'Oreal")
                .category("HAIRCARE").price(BigDecimal.valueOf(450))
                .stock(100).isActive(true)
                .build();
    }

    // ── getAllProducts ─────────────────────────────────────────────────────────

    @Test
    void getAllProducts_NoFilters_ReturnsAllActive() {
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product));
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(4.5);
        when(reviewRepository.countByProductId(10L)).thenReturn(3L);
        when(favoriteProductRepository.findByCustomerId(1L)).thenReturn(List.of());

        Pageable pageable = PageRequest.of(0, 12);
        Page<ProductResponseDTO> result = productService.getAllProducts(
                null, null, null, null, 1L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Argan Oil Shampoo", result.getContent().get(0).getName());
    }

    @Test
    void getAllProducts_WithCategoryFilter_ReturnsFiltered() {
        Product skinProduct = Product.builder().id(20L).name("Vitamin C Serum")
                .brand("Minimalist").category("SKINCARE")
                .price(BigDecimal.valueOf(890)).stock(50).isActive(true).build();
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product, skinProduct));
        when(reviewRepository.findAverageRatingByProductId(anyLong())).thenReturn(0.0);
        when(reviewRepository.countByProductId(anyLong())).thenReturn(0L);
        when(favoriteProductRepository.findByCustomerId(anyLong())).thenReturn(List.of());

        Page<ProductResponseDTO> result = productService.getAllProducts(
                "SKINCARE", null, null, null, 1L, PageRequest.of(0, 12));

        assertEquals(1, result.getTotalElements());
        assertEquals("Vitamin C Serum", result.getContent().get(0).getName());
    }

    @Test
    void getAllProducts_WithPriceRange_ReturnsFiltered() {
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product));
        when(reviewRepository.findAverageRatingByProductId(anyLong())).thenReturn(0.0);
        when(reviewRepository.countByProductId(anyLong())).thenReturn(0L);
        when(favoriteProductRepository.findByCustomerId(anyLong())).thenReturn(List.of());

        // Price 450 is within 400-500
        Page<ProductResponseDTO> inRange = productService.getAllProducts(
                null, BigDecimal.valueOf(400), BigDecimal.valueOf(500), null, 1L, PageRequest.of(0, 12));
        assertEquals(1, inRange.getTotalElements());

        // Price 450 is outside 500-600
        Page<ProductResponseDTO> outOfRange = productService.getAllProducts(
                null, BigDecimal.valueOf(500), BigDecimal.valueOf(600), null, 1L, PageRequest.of(0, 12));
        assertEquals(0, outOfRange.getTotalElements());
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_Success_DeductsStockAndReturnsDTO() {
        ProductOrderRequest req = new ProductOrderRequest(10L, 2);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        ProductOrder savedOrder = ProductOrder.builder()
                .id(100L).customer(customer).product(product)
                .quantity(2).unitPrice(BigDecimal.valueOf(450))
                .totalPrice(BigDecimal.valueOf(900))
                .status(ProductOrderStatus.PLACED)
                .orderDate(LocalDateTime.now())
                .build();
        when(orderRepository.save(any())).thenReturn(savedOrder);
        doNothing().when(loyaltyService).awardPointsForProductPurchase(anyLong(), any());

        ProductOrderResponseDTO result = productService.placeOrder(1L, req);

        assertNotNull(result);
        assertEquals(100L, result.getOrderId());
        assertEquals("PLACED", result.getStatus());
        assertEquals(BigDecimal.valueOf(900), result.getTotalPrice());
        verify(productRepository).save(argThat(p -> p.getStock() == 98)); // 100 - 2
    }

    @Test
    void placeOrder_InsufficientStock_ThrowsInvalidOperation() {
        product.setStock(1);
        ProductOrderRequest req = new ProductOrderRequest(10L, 5);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(InvalidOperationException.class,
                () -> productService.placeOrder(1L, req));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_ProductNotFound_ThrowsResourceNotFound() {
        ProductOrderRequest req = new ProductOrderRequest(999L, 1);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.placeOrder(1L, req));
    }

    @Test
    void placeOrder_InactiveProduct_ThrowsInvalidOperation() {
        product.setActive(false);
        ProductOrderRequest req = new ProductOrderRequest(10L, 1);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(InvalidOperationException.class,
                () -> productService.placeOrder(1L, req));
    }

    // ── addToFavorites ────────────────────────────────────────────────────────

    @Test
    void addToFavorites_Success_ReturnsFavoriteDTO() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(false);
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(0.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(0L);

        FavoriteProduct saved = FavoriteProduct.builder()
                .id(5L).customer(customer).product(product)
                .createdAt(LocalDateTime.now()).build();
        when(favoriteProductRepository.save(any())).thenReturn(saved);

        FavoriteResponseDTO result = productService.addToFavorites(1L, 10L);

        assertNotNull(result);
        assertEquals(5L, result.getFavoriteId());
        assertEquals("Argan Oil Shampoo", result.getProduct().getName());
    }

    @Test
    void addToFavorites_Duplicate_ThrowsConflict() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> productService.addToFavorites(1L, 10L));
        verify(favoriteProductRepository, never()).save(any());
    }

    // ── addReview ─────────────────────────────────────────────────────────────

    @Test
    void addReview_Success_ReturnsReviewDTO() {
        ProductReviewRequest req = new ProductReviewRequest(5, "Excellent product!");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);
        when(reviewRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(false);

        ProductReview saved = ProductReview.builder()
                .id(50L).customer(customer).product(product)
                .rating(5).reviewText("Excellent product!")
                .createdAt(LocalDateTime.now()).build();
        when(reviewRepository.save(any())).thenReturn(saved);

        ProductReviewResponseDTO result = productService.addReview(1L, 10L, req);

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Excellent product!", result.getReviewText());
        assertEquals("Alice", result.getCustomerName());
    }

    @Test
    void addReview_NotOrdered_ThrowsInvalidOperation() {
        ProductReviewRequest req = new ProductReviewRequest(4, "Good product");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(false);

        assertThrows(InvalidOperationException.class,
                () -> productService.addReview(1L, 10L, req));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_AlreadyReviewed_ThrowsConflict() {
        ProductReviewRequest req = new ProductReviewRequest(3, "Average");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);
        when(reviewRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> productService.addReview(1L, 10L, req));
    }
}
