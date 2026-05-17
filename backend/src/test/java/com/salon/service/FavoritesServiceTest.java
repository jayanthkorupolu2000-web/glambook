package com.salon.service;

import com.salon.dto.response.ProductResponse;
import com.salon.dto.response.ServiceResponse;
import com.salon.entity.*;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritesServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private FavoriteProductRepository favoriteProductRepository;
    @Mock private FavoriteServiceRepository favoriteServiceRepository;

    @InjectMocks private FavoritesService favoritesService;

    private Customer customer;
    private Product product;
    private com.salon.entity.Service service;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice").build();

        product = Product.builder()
                .id(10L).name("Shampoo").brand("Dove")
                .category("Hair").price(new BigDecimal("250.00")).stock(50).build();

        service = com.salon.entity.Service.builder()
                .id(20L).name("Haircut").category("Hair")
                .gender(Gender.WOMEN).price(new BigDecimal("300.00")).durationMins(45).build();
    }

    // ── toggleFavoriteProduct ─────────────────────────────────────────────────

    @Test
    void toggleFavoriteProduct_NotFavorited_ShouldAdd() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(false);

        favoritesService.toggleFavoriteProduct(1L, 10L);

        verify(favoriteProductRepository).save(any(FavoriteProduct.class));
        verify(favoriteProductRepository, never()).deleteByCustomerIdAndProductId(any(), any());
    }

    @Test
    void toggleFavoriteProduct_AlreadyFavorited_ShouldRemove() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);

        favoritesService.toggleFavoriteProduct(1L, 10L);

        verify(favoriteProductRepository).deleteByCustomerIdAndProductId(1L, 10L);
        verify(favoriteProductRepository, never()).save(any());
    }

    @Test
    void toggleFavoriteProduct_CustomerNotFound_ShouldThrow() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> favoritesService.toggleFavoriteProduct(99L, 10L));
    }

    @Test
    void toggleFavoriteProduct_ProductNotFound_ShouldThrow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> favoritesService.toggleFavoriteProduct(1L, 99L));
    }

    // ── getFavoriteProducts ───────────────────────────────────────────────────

    @Test
    void getFavoriteProducts_ShouldReturnMappedList() {
        FavoriteProduct fp = FavoriteProduct.builder()
                .id(1L).customer(customer).product(product).build();

        when(favoriteProductRepository.findByCustomerId(1L)).thenReturn(List.of(fp));

        List<ProductResponse> result = favoritesService.getFavoriteProducts(1L);

        assertEquals(1, result.size());
        assertEquals("Shampoo", result.get(0).getName());
        assertTrue(result.get(0).isFavorited());
    }

    @Test
    void getFavoriteProducts_Empty_ShouldReturnEmptyList() {
        when(favoriteProductRepository.findByCustomerId(1L)).thenReturn(List.of());

        List<ProductResponse> result = favoritesService.getFavoriteProducts(1L);

        assertTrue(result.isEmpty());
    }

    // ── isProductFavorited ────────────────────────────────────────────────────

    @Test
    void isProductFavorited_True_ShouldReturnTrue() {
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(true);

        assertTrue(favoritesService.isProductFavorited(1L, 10L));
    }

    @Test
    void isProductFavorited_False_ShouldReturnFalse() {
        when(favoriteProductRepository.existsByCustomerIdAndProductId(1L, 10L)).thenReturn(false);

        assertFalse(favoritesService.isProductFavorited(1L, 10L));
    }

    // ── toggleFavoriteService ─────────────────────────────────────────────────

    @Test
    void toggleFavoriteService_NotFavorited_ShouldAdd() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(20L)).thenReturn(Optional.of(service));
        when(favoriteServiceRepository.existsByCustomerIdAndServiceId(1L, 20L)).thenReturn(false);

        favoritesService.toggleFavoriteService(1L, 20L);

        verify(favoriteServiceRepository).save(any(FavoriteService.class));
    }

    @Test
    void toggleFavoriteService_AlreadyFavorited_ShouldRemove() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(20L)).thenReturn(Optional.of(service));
        when(favoriteServiceRepository.existsByCustomerIdAndServiceId(1L, 20L)).thenReturn(true);

        favoritesService.toggleFavoriteService(1L, 20L);

        verify(favoriteServiceRepository).deleteByCustomerIdAndServiceId(1L, 20L);
        verify(favoriteServiceRepository, never()).save(any());
    }

    // ── getFavoriteServices ───────────────────────────────────────────────────

    @Test
    void getFavoriteServices_ShouldReturnMappedList() {
        FavoriteService fs = FavoriteService.builder()
                .id(1L).customer(customer).service(service).build();

        when(favoriteServiceRepository.findByCustomerId(1L)).thenReturn(List.of(fs));

        List<ServiceResponse> result = favoritesService.getFavoriteServices(1L);

        assertEquals(1, result.size());
        assertEquals("Haircut", result.get(0).getName());
        assertTrue(result.get(0).isFavorited());
    }

    // ── isServiceFavorited ────────────────────────────────────────────────────

    @Test
    void isServiceFavorited_ShouldReturnCorrectValue() {
        when(favoriteServiceRepository.existsByCustomerIdAndServiceId(1L, 20L)).thenReturn(true);

        assertTrue(favoritesService.isServiceFavorited(1L, 20L));
    }

    // ── enrichProducts ────────────────────────────────────────────────────────

    @Test
    void enrichProducts_NullCustomerId_ShouldReturnUnchanged() {
        ProductResponse pr = new ProductResponse();
        pr.setId(10L); pr.setName("Shampoo");

        List<ProductResponse> result = favoritesService.enrichProducts(List.of(pr), null);

        assertFalse(result.get(0).isFavorited());
    }

    @Test
    void enrichProducts_WithFavorites_ShouldSetFavoritedFlag() {
        FavoriteProduct fp = FavoriteProduct.builder()
                .id(1L).customer(customer).product(product).build();

        when(favoriteProductRepository.findByCustomerId(1L)).thenReturn(List.of(fp));

        ProductResponse pr = new ProductResponse();
        pr.setId(10L); pr.setName("Shampoo");

        List<ProductResponse> result = favoritesService.enrichProducts(List.of(pr), 1L);

        assertTrue(result.get(0).isFavorited());
    }
}
