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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductOrderRepository orderRepository;
    @Mock private FavoriteProductRepository favoriteProductRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private LoyaltyService loyaltyService;
    @Mock private WalletService walletService;

    @InjectMocks private ProductServiceImpl productService;

    private Customer customer;
    private Product product;
    private ProductOrder order;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice")
                .email("alice@gmail.com").status(UserStatus.ACTIVE).build();

        product = Product.builder()
                .id(10L).name("Shampoo").brand("Dove").category("HAIRCARE")
                .description("Moisturising shampoo").price(new BigDecimal("250.00"))
                .stock(50).isActive(true).build();

        order = ProductOrder.builder()
                .id(100L).customer(customer).product(product)
                .quantity(2).unitPrice(new BigDecimal("250.00"))
                .totalPrice(new BigDecimal("500.00"))
                .status(ProductOrderStatus.PLACED).build();
    }

    // ── getAllProducts ────────────────────────────────────────────────────────

    @Test
    void getAllProducts_NoFilters_ShouldReturnAllActive() {
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product));
        when(favoriteProductRepository.findByCustomerId(1L)).thenReturn(List.of());
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(4.5);
        when(reviewRepository.countByProductId(10L)).thenReturn(10L);

        Page<ProductResponseDTO> page = productService.getAllProducts(
                null, null, null, null, 1L, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Shampoo", page.getContent().get(0).getName());
    }

    @Test
    void getAllProducts_FilterByCategory_ShouldReturnMatching() {
        Product other = Product.builder().id(11L).name("Lipstick").brand("MAC")
                .category("MAKEUP").price(new BigDecimal("500.00")).stock(20).isActive(true).build();

        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product, other));
        when(favoriteProductRepository.findByCustomerId(1L)).thenReturn(List.of());
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(0.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(0L);

        Page<ProductResponseDTO> page = productService.getAllProducts(
                "HAIRCARE", null, null, null, 1L, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Shampoo", page.getContent().get(0).getName());
    }

    // ── getProductById ────────────────────────────────────────────────────────

    @Test
    void getProductById_Exists_ShouldReturnDTO() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteProductRepository.findByCustomerId(1L)).thenReturn(List.of());
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(4.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(5L);

        ProductResponseDTO dto = productService.getProductById(10L, 1L);

        assertNotNull(dto);
        assertEquals("Shampoo", dto.getName());
        assertEquals(4.0, dto.getAverageRating());
    }

    @Test
    void getProductById_NotFound_ShouldThrow() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(99L, 1L));
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_Valid_ShouldDeductStockAndSave() {
        ProductOrderRequest req = new ProductOrderRequest();
        req.setProductId(10L); req.setQuantity(2);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any(ProductOrder.class))).thenReturn(order);

        ProductOrderResponseDTO response = productService.placeOrder(1L, req);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("PLACED", response.getStatus());
        // Stock should have been decremented
        verify(productRepository).save(argThat(p -> p.getStock() == 48));
    }

    @Test
    void placeOrder_InsufficientStock_ShouldThrow() {
        product.setStock(1);
        ProductOrderRequest req = new ProductOrderRequest();
        req.setProductId(10L); req.setQuantity(5);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(InvalidOperationException.class,
                () -> productService.placeOrder(1L, req));
    }

    @Test
    void placeOrder_SuspendedCustomer_ShouldThrow() {
        customer.setStatus(UserStatus.SUSPENDED);
        ProductOrderRequest req = new ProductOrderRequest();
        req.setProductId(10L); req.setQuantity(1);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThrows(InvalidOperationException.class,
                () -> productService.placeOrder(1L, req));
    }

    @Test
    void placeOrder_CustomerNotFound_ShouldThrow() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.placeOrder(99L, new ProductOrderRequest()));
    }

    // ── payOrder ─────────────────────────────────────────────────────────────

    @Test
    void payOrder_Valid_ShouldMarkDeliveredAndAwardPoints() {
        ProductOrder delivered = ProductOrder.builder()
                .id(100L).customer(customer).product(product)
                .quantity(2).unitPrice(new BigDecimal("250.00"))
                .totalPrice(new BigDecimal("500.00"))
                .status(ProductOrderStatus.DELIVERED).build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(delivered);
        doNothing().when(loyaltyService).awardPointsForProductPurchase(anyLong(), any());

        ProductOrderResponseDTO response = productService.payOrder(1L, 100L, "CASH");

        assertEquals("DELIVERED", response.getStatus());
        verify(loyaltyService).awardPointsForProductPurchase(eq(1L), eq(new BigDecimal("500.00")));
    }

    @Test
    void payOrder_AlreadyDelivered_ShouldThrow() {
        order.setStatus(ProductOrderStatus.DELIVERED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOperationException.class,
                () -> productService.payOrder(1L, 100L, "CASH"));
    }

    @Test
    void payOrder_CancelledOrder_ShouldThrow() {
        order.setStatus(ProductOrderStatus.CANCELLED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOperationException.class,
                () -> productService.payOrder(1L, 100L, "CASH"));
    }

    @Test
    void payOrder_WrongCustomer_ShouldThrow() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOperationException.class,
                () -> productService.payOrder(99L, 100L, "CASH"));
    }

    // ── addReview ─────────────────────────────────────────────────────────────

    @Test
    void addReview_Valid_ShouldSaveAndReturn() {
        ProductReviewRequest req = new ProductReviewRequest();
        req.setRating(5); req.setReviewText("Excellent!");

        ProductReview saved = ProductReview.builder()
                .id(1L).customer(customer).product(product)
                .rating(5).reviewText("Excellent!").build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.existsDeliveredOrderByCustomerIdAndProductId(1L, 10L)).thenReturn(true);
        when(reviewRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(false);
        when(reviewRepository.save(any(ProductReview.class))).thenReturn(saved);

        ProductReviewResponseDTO response = productService.addReview(1L, 10L, req);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Excellent!", response.getReviewText());
    }

    @Test
    void addReview_NoDeliveredOrder_ShouldThrow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.existsDeliveredOrderByCustomerIdAndProductId(1L, 10L)).thenReturn(false);

        assertThrows(InvalidOperationException.class,
                () -> productService.addReview(1L, 10L, new ProductReviewRequest()));
    }

    @Test
    void addReview_AlreadyReviewed_ShouldThrow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.existsDeliveredOrderByCustomerIdAndProductId(1L, 10L)).thenReturn(true);
        when(reviewRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> productService.addReview(1L, 10L, new ProductReviewRequest()));
    }

    // ── addToFavorites ────────────────────────────────────────────────────────

    @Test
    void addToFavorites_Valid_ShouldSaveAndReturn() {
        FavoriteProduct fp = FavoriteProduct.builder()
                .id(1L).customer(customer).product(product).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(false);
        when(favoriteProductRepository.save(any())).thenReturn(fp);
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(0.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(0L);

        FavoriteResponseDTO response = productService.addToFavorites(1L, 10L);

        assertNotNull(response);
        assertEquals(1L, response.getFavoriteId());
    }

    @Test
    void addToFavorites_AlreadyFavorited_ShouldThrowConflictException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> productService.addToFavorites(1L, 10L));
    }

    // ── removeFromFavorites ───────────────────────────────────────────────────

    @Test
    void removeFromFavorites_Exists_ShouldDelete() {
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);

        productService.removeFromFavorites(1L, 10L);

        verify(favoriteProductRepository).deleteByCustomerIdAndProductId(1L, 10L);
    }

    @Test
    void removeFromFavorites_NotFound_ShouldThrow() {
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> productService.removeFromFavorites(1L, 10L));
    }

    // ── searchProducts ────────────────────────────────────────────────────────

    @Test
    void searchProducts_BlankKeyword_ShouldThrow() {
        assertThrows(InvalidOperationException.class,
                () -> productService.searchProducts("  ", 1L));
    }

    @Test
    void searchProducts_ValidKeyword_ShouldReturnResults() {
        when(productRepository.searchProducts("shampoo")).thenReturn(List.of(product));
        when(favoriteProductRepository.findByCustomerId(1L)).thenReturn(List.of());
        when(reviewRepository.findAverageRatingByProductId(10L)).thenReturn(0.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(0L);

        List<ProductResponseDTO> results = productService.searchProducts("shampoo", 1L);

        assertEquals(1, results.size());
        assertEquals("Shampoo", results.get(0).getName());
    }

    // ── getOrderHistory ───────────────────────────────────────────────────────

    @Test
    void getOrderHistory_ShouldReturnList() {
        when(orderRepository.findByCustomerIdOrderByOrderDateDesc(1L)).thenReturn(List.of(order));

        List<ProductOrderResponseDTO> history = productService.getOrderHistory(1L);

        assertEquals(1, history.size());
        assertEquals(100L, history.get(0).getOrderId());
    }
}
