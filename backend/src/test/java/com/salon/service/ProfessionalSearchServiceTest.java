package com.salon.service;

import com.salon.dto.response.ProfessionalProfileResponse;
import com.salon.entity.Professional;
import com.salon.entity.UserStatus;
import com.salon.repository.PortfolioRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.ReviewRepository;
import com.salon.repository.ServiceRepository;
import com.salon.service.impl.ProfessionalProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalSearchServiceTest {

    @Mock ProfessionalRepository professionalRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock PortfolioRepository portfolioRepository;
    @Mock ServiceRepository serviceRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks ProfessionalProfileServiceImpl searchService;

    private Professional makePro(Long id, String city, boolean availableHome) {
        Professional p = new Professional();
        p.setId(id);
        p.setName("Pro " + id);
        p.setEmail("pro" + id + "@test.com");
        p.setCity(city);
        p.setStatus(UserStatus.ACTIVE);
        p.setAvailableHome(availableHome);
        p.setAvailableSalon(true);
        return p;
    }

    private com.salon.entity.Service makeService(Long profId, BigDecimal price) {
        com.salon.entity.Service s = new com.salon.entity.Service();
        s.setId(profId * 10);
        s.setName("Haircut");
        s.setPrice(price);
        s.setIsActive(true);
        s.setProfessionalId(profId);
        return s;
    }

    @BeforeEach
    void setUp() {
        // Default: no reviews, no portfolio
        when(reviewRepository.findAverageRatingByProfessionalId(anyLong())).thenReturn(4.0);
        when(reviewRepository.countByProfessionalId(anyLong())).thenReturn(5L);
        when(portfolioRepository.findByProfessionalIdAndIsFeatured(anyLong(), anyBoolean()))
                .thenReturn(List.of());
    }

    // ── searchProfessionals_priceRange_success ────────────────────────────────

    @Test
    @DisplayName("searchProfessionalsWithPrice: returns only professionals with services in price range")
    void searchProfessionals_priceRange_success() {
        Professional p1 = makePro(1L, "Hyderabad", false);
        Professional p2 = makePro(2L, "Hyderabad", false);

        when(professionalRepository.findAll()).thenReturn(List.of(p1, p2));

        // p1 has a ₹300 service, p2 has a ₹800 service
        when(serviceRepository.findByProfessionalId(1L))
                .thenReturn(List.of(makeService(1L, new BigDecimal("300"))));
        when(serviceRepository.findByProfessionalId(2L))
                .thenReturn(List.of(makeService(2L, new BigDecimal("800"))));

        // Filter: minPrice=200, maxPrice=500 → only p1 should match
        List<ProfessionalProfileResponse> result = searchService.searchProfessionalsWithPrice(
                "Hyderabad", null, null, null, null, 200.0, 500.0, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("searchProfessionalsWithPrice: no price filter returns all active professionals")
    void searchProfessionals_noPriceFilter_returnsAll() {
        Professional p1 = makePro(1L, "Hyderabad", false);
        Professional p2 = makePro(2L, "Hyderabad", false);

        when(professionalRepository.findAll()).thenReturn(List.of(p1, p2));
        when(serviceRepository.findByProfessionalId(anyLong())).thenReturn(List.of());

        List<ProfessionalProfileResponse> result = searchService.searchProfessionalsWithPrice(
                "Hyderabad", null, null, null, null, null, null, null);

        assertThat(result).hasSize(2);
    }

    // ── searchProfessionals_noResults ─────────────────────────────────────────

    @Test
    @DisplayName("searchProfessionalsWithPrice: returns empty list when no professionals match price range")
    void searchProfessionals_noResults_emptyList() {
        Professional p1 = makePro(1L, "Hyderabad", false);

        when(professionalRepository.findAll()).thenReturn(List.of(p1));
        when(serviceRepository.findByProfessionalId(1L))
                .thenReturn(List.of(makeService(1L, new BigDecimal("1500"))));

        // Filter: max ₹500 — p1's service is ₹1500, no match
        List<ProfessionalProfileResponse> result = searchService.searchProfessionalsWithPrice(
                "Hyderabad", null, null, null, null, null, 500.0, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchProfessionalsWithPrice: rating filter excludes low-rated professionals")
    void searchProfessionals_ratingFilter_excludesLowRated() {
        Professional p1 = makePro(1L, "Hyderabad", false);
        Professional p2 = makePro(2L, "Hyderabad", false);

        when(professionalRepository.findAll()).thenReturn(List.of(p1, p2));
        when(serviceRepository.findByProfessionalId(anyLong())).thenReturn(List.of());

        // p1 has 4.5 rating, p2 has 2.0
        when(reviewRepository.findAverageRatingByProfessionalId(1L)).thenReturn(4.5);
        when(reviewRepository.findAverageRatingByProfessionalId(2L)).thenReturn(2.0);

        List<ProfessionalProfileResponse> result = searchService.searchProfessionalsWithPrice(
                "Hyderabad", null, null, null, null, null, null, 4.0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }
}
