package com.salon.service.impl;

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
import com.salon.service.LoyaltyService;
import com.salon.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final int POINTS_PER_100_RUPEES = 10;

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;
    private final ProductOrderRepository orderRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final LoyaltyService loyaltyService;

    // ── Browse / Search ───────────────────────────────────────────────────────

    @Override
    public Page<ProductResponseDTO> getAllProducts(String category, BigDecimal minPrice,
                                                   BigDecimal maxPrice, String brand,
                                                   Long customerId, Pageable pageable) {
        List<Product> all = productRepository.findByIsActiveTrue();

        List<Product> filtered = all.stream()
                .filter(p -> category == null || category.isBlank()
                        || p.getCategory().equalsIgnoreCase(category))
                .filter(p -> brand == null || brand.isBlank()
                        || p.getBrand().toLowerCase().contains(brand.toLowerCase()))
                .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || maxPrice.compareTo(BigDecimal.ZERO) <= 0
                        || p.getPrice().compareTo(maxPrice) <= 0)
                .collect(Collectors.toList());

        Set<Long> favIds = getFavIds(customerId);

        List<ProductResponseDTO> dtos = filtered.stream()
                .map(p -> toDTO(p, favIds))
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), dtos.size());
        List<ProductResponseDTO> page = start >= dtos.size() ? List.of() : dtos.subList(start, end);
        return new PageImpl<>(page, pageable, dtos.size());
    }

    @Override
    public ProductResponseDTO getProductById(Long productId, Long customerId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        Set<Long> favIds = getFavIds(customerId);
        return toDTO(p, favIds);
    }

    @Override
    public List<ProductResponseDTO> searchProducts(String keyword, Long customerId) {
        if (keyword == null || keyword.isBlank())
            throw new InvalidOperationException("Please provide a valid keyword");
        Set<Long> favIds = getFavIds(customerId);
        return productRepository.searchProducts(keyword).stream()
                .map(p -> toDTO(p, favIds))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponseDTO> getRecommendedProducts(Long customerId) {
        // Recommend products whose category matches the customer's past appointment service categories
        List<Appointment> appointments = appointmentRepository.findByCustomerId(customerId);
        Set<String> serviceCategories = appointments.stream()
                .filter(a -> a.getService() != null && a.getService().getCategory() != null)
                .map(a -> a.getService().getCategory().toUpperCase())
                .collect(Collectors.toSet());

        Set<Long> favIds = getFavIds(customerId);
        List<Product> all = productRepository.findByIsActiveTrue();

        // Map service categories to product categories
        Map<String, String> categoryMap = Map.of(
                "HAIR", "HAIRCARE",
                "HAIRCARE", "HAIRCARE",
                "SKIN", "SKINCARE",
                "SKINCARE", "SKINCARE",
                "MAKEUP", "MAKEUP",
                "NAIL", "NAILCARE",
                "NAILCARE", "NAILCARE",
                "FRAGRANCE", "FRAGRANCE"
        );

        Set<String> productCategories = serviceCategories.stream()
                .map(sc -> categoryMap.getOrDefault(sc, sc))
                .collect(Collectors.toSet());

        List<Product> recommended = all.stream()
                .filter(p -> productCategories.contains(p.getCategory().toUpperCase()))
                .collect(Collectors.toList());

        // Fall back to top-rated products if no match
        if (recommended.isEmpty()) {
            recommended = all.stream()
                    .sorted(Comparator.comparingDouble(
                            p -> -reviewRepository.findAverageRatingByProductId(p.getId())))
                    .limit(6)
                    .collect(Collectors.toList());
        }

        return recommended.stream().map(p -> toDTO(p, favIds)).collect(Collectors.toList());
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductOrderResponseDTO placeOrder(Long customerId, ProductOrderRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        if (!product.isActive())
            throw new InvalidOperationException("Product is not available");

        if (product.getStock() < request.getQuantity())
            throw new InvalidOperationException(
                    "Insufficient stock. Available: " + product.getStock());

        // Deduct stock
        product.setStock(product.getStock() - request.getQuantity());
        productRepository.save(product);

        BigDecimal unitPrice  = product.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        ProductOrder order = ProductOrder.builder()
                .customer(customer)
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .status(ProductOrderStatus.PLACED)
                .build();

        ProductOrder saved = orderRepository.save(order);
        log.info("Product order {} placed by customer {} for product {}",
                saved.getId(), customerId, product.getId());

        // Do NOT award loyalty points here — they are awarded on payment (payOrder)
        return toOrderDTO(saved, 0);
    }

    @Override
    public List<ProductOrderResponseDTO> getOrderHistory(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream().map(o -> toOrderDTO(o, 0)).collect(Collectors.toList());
    }

    @Override
    public ProductOrderResponseDTO getOrderById(Long customerId, Long orderId) {
        ProductOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getCustomer().getId().equals(customerId))
            throw new InvalidOperationException("Order does not belong to this customer");
        return toOrderDTO(order, 0);
    }

    @Override
    @Transactional
    public ProductOrderResponseDTO payOrder(Long customerId, Long orderId, String method) {
        ProductOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getCustomer().getId().equals(customerId))
            throw new InvalidOperationException("Order does not belong to this customer");
        if (order.getStatus() == ProductOrderStatus.CANCELLED)
            throw new InvalidOperationException("Cannot pay for a cancelled order");
        if (order.getStatus() == ProductOrderStatus.DELIVERED)
            throw new InvalidOperationException("Order has already been paid");

        // Mark as DELIVERED (payment = delivery for online product orders)
        order.setStatus(ProductOrderStatus.DELIVERED);
        order.setDeliveryDate(java.time.LocalDate.now());
        ProductOrder saved = orderRepository.save(order);

        log.info("Product order {} paid via {} by customer {} — marked DELIVERED",
                orderId, method, customerId);

        // Award loyalty points on payment: ₹100 = 10 pts
        int points = saved.getTotalPrice()
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(POINTS_PER_100_RUPEES)).intValue();

        if (points > 0) {
            try {
                loyaltyService.awardPointsForProductPurchase(customerId, saved.getTotalPrice());
            } catch (Exception e) {
                // Non-fatal — log and continue; don't roll back the payment
                log.warn("Could not award loyalty points for order {}: {}", orderId, e.getMessage());
            }
        }

        return toOrderDTO(saved, points);
    }

    // ── Favorites ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public FavoriteResponseDTO addToFavorites(Long customerId, Long productId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (favoriteProductRepository.existsByCustomerIdAndProductId(customerId, productId))
            throw new ConflictException("Product already in favorites");

        FavoriteProduct fav = favoriteProductRepository.save(
                FavoriteProduct.builder().customer(customer).product(product).build());

        return FavoriteResponseDTO.builder()
                .favoriteId(fav.getId())
                .product(toDTO(product, Set.of(productId)))
                .addedAt(fav.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void removeFromFavorites(Long customerId, Long productId) {
        if (!favoriteProductRepository.existsByCustomerIdAndProductId(customerId, productId))
            throw new ResourceNotFoundException("Favorite not found");
        favoriteProductRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Override
    public List<FavoriteResponseDTO> getFavorites(Long customerId) {
        return favoriteProductRepository.findByCustomerId(customerId).stream()
                .map(fp -> FavoriteResponseDTO.builder()
                        .favoriteId(fp.getId())
                        .product(toDTO(fp.getProduct(), Set.of(fp.getProduct().getId())))
                        .addedAt(fp.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductReviewResponseDTO addReview(Long customerId, Long productId,
                                              ProductReviewRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        // Must have a DELIVERED order for this product
        if (!orderRepository.existsDeliveredOrderByCustomerIdAndProductId(customerId, productId))
            throw new InvalidOperationException(
                    "You can only review products from a delivered order");

        if (reviewRepository.existsByCustomerIdAndProductId(customerId, productId))
            throw new ConflictException("You have already reviewed this product");

        ProductReview review = reviewRepository.save(ProductReview.builder()
                .customer(customer)
                .product(product)
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .build());

        return toReviewDTO(review);
    }

    @Override
    public List<ProductReviewResponseDTO> getReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toReviewDTO).collect(Collectors.toList());
    }

    @Override
    public boolean hasDeliveredOrder(Long customerId, Long productId) {
        return orderRepository.existsDeliveredOrderByCustomerIdAndProductId(customerId, productId);
    }

    @Override
    public boolean hasReviewed(Long customerId, Long productId) {
        return reviewRepository.existsByCustomerIdAndProductId(customerId, productId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Set<Long> getFavIds(Long customerId) {
        if (customerId == null) return Set.of();
        return favoriteProductRepository.findByCustomerId(customerId).stream()
                .map(fp -> fp.getProduct().getId())
                .collect(Collectors.toSet());
    }

    private ProductResponseDTO toDTO(Product p, Set<Long> favIds) {
        double avg = reviewRepository.findAverageRatingByProductId(p.getId());
        long count = reviewRepository.countByProductId(p.getId());
        return ProductResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .category(p.getCategory())
                .brand(p.getBrand())
                .price(p.getPrice())
                .stockQuantity(p.getStock())
                .imageUrl(p.getImageUrl())
                .ingredients(p.getIngredients())
                .usageTips(p.getUsageTips())
                .averageRating(Math.round(avg * 10.0) / 10.0)
                .reviewCount(count)
                .favorited(favIds.contains(p.getId()))
                .build();
    }

    private ProductOrderResponseDTO toOrderDTO(ProductOrder o, int loyaltyPoints) {
        return ProductOrderResponseDTO.builder()
                .orderId(o.getId())
                .productName(o.getProduct().getName())
                .productImageUrl(o.getProduct().getImageUrl())
                .quantity(o.getQuantity())
                .unitPrice(o.getUnitPrice())
                .totalPrice(o.getTotalPrice())
                .status(o.getStatus().name())
                .trackingNumber(o.getTrackingNumber())
                .orderDate(o.getOrderDate())
                .loyaltyPointsEarned(loyaltyPoints)
                .build();
    }

    private ProductReviewResponseDTO toReviewDTO(ProductReview r) {
        return ProductReviewResponseDTO.builder()
                .id(r.getId())
                .customerId(r.getCustomer().getId())
                .customerName(r.getCustomer().getName())
                .rating(r.getRating())
                .reviewText(r.getReviewText())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
