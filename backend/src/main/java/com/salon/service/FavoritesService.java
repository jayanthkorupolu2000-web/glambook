package com.salon.service;

import com.salon.dto.response.ProductResponse;
import com.salon.dto.response.ServiceResponse;
import com.salon.entity.*;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoritesService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final FavoriteServiceRepository favoriteServiceRepository;

    // ─── Products ────────────────────────────────────────────────────────────

    @Transactional
    public void toggleFavoriteProduct(Long customerId, Long productId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (favoriteProductRepository.existsByCustomerIdAndProductId(customerId, productId)) {
            favoriteProductRepository.deleteByCustomerIdAndProductId(customerId, productId);
        } else {
            favoriteProductRepository.save(
                    FavoriteProduct.builder().customer(customer).product(product).build());
        }
    }

    public List<ProductResponse> getFavoriteProducts(Long customerId) {
        return favoriteProductRepository.findByCustomerId(customerId)
                .stream()
                .map(fp -> toProductResponse(fp.getProduct(), true))
                .collect(Collectors.toList());
    }

    public boolean isProductFavorited(Long customerId, Long productId) {
        return favoriteProductRepository.existsByCustomerIdAndProductId(customerId, productId);
    }

    // ─── Services ────────────────────────────────────────────────────────────

    @Transactional
    public void toggleFavoriteService(Long customerId, Long serviceId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        com.salon.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        if (favoriteServiceRepository.existsByCustomerIdAndServiceId(customerId, serviceId)) {
            favoriteServiceRepository.deleteByCustomerIdAndServiceId(customerId, serviceId);
        } else {
            favoriteServiceRepository.save(
                    FavoriteService.builder().customer(customer).service(service).build());
        }
    }

    public List<ServiceResponse> getFavoriteServices(Long customerId) {
        return favoriteServiceRepository.findByCustomerId(customerId)
                .stream()
                .map(fs -> toServiceResponse(fs.getService(), true))
                .collect(Collectors.toList());
    }

    public boolean isServiceFavorited(Long customerId, Long serviceId) {
        return favoriteServiceRepository.existsByCustomerIdAndServiceId(customerId, serviceId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Enrich a product list with favorited flags for a given customer. */
    public List<ProductResponse> enrichProducts(List<ProductResponse> products, Long customerId) {
        if (customerId == null) return products;
        Set<Long> favIds = favoriteProductRepository.findByCustomerId(customerId)
                .stream().map(fp -> fp.getProduct().getId()).collect(Collectors.toSet());
        products.forEach(p -> p.setFavorited(favIds.contains(p.getId())));
        return products;
    }

    /** Enrich a service list with favorited flags for a given customer. */
    public List<ServiceResponse> enrichServices(List<ServiceResponse> services, Long customerId) {
        if (customerId == null) return services;
        Set<Long> favIds = favoriteServiceRepository.findByCustomerId(customerId)
                .stream().map(fs -> fs.getService().getId()).collect(Collectors.toSet());
        services.forEach(s -> s.setFavorited(favIds.contains(s.getId())));
        return services;
    }

    private ProductResponse toProductResponse(Product p, boolean favorited) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setBrand(p.getBrand());
        r.setCategory(p.getCategory());
        r.setDescription(p.getDescription());
        r.setIngredients(p.getIngredients());
        r.setUsageTips(p.getUsageTips());
        r.setPrice(p.getPrice());
        r.setStock(p.getStock());
        r.setImageUrl(p.getImageUrl());
        r.setRecommendedFor(p.getRecommendedFor());
        r.setFavorited(favorited);
        return r;
    }

    private ServiceResponse toServiceResponse(com.salon.entity.Service s, boolean favorited) {
        return ServiceResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .category(s.getCategory())
                .gender(s.getGender() != null ? s.getGender().name() : null)
                .price(s.getPrice())
                .durationMins(s.getDurationMins())
                .isActive(s.getIsActive())
                .favorited(favorited)
                .professionalId(s.getProfessionalId())
                .build();
    }
}
