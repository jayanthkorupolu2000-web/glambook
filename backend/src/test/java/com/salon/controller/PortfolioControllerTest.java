package com.salon.controller;

import com.salon.dto.response.PortfolioResponse;
import com.salon.entity.Portfolio;
import com.salon.entity.PortfolioMediaType;
import com.salon.entity.Professional;
import com.salon.repository.PortfolioRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PortfolioRepository portfolioRepository;
    @MockBean ProfessionalRepository professionalRepository;
    @MockBean FileStorageService fileStorageService;
    @MockBean com.salon.security.JwtUtil jwtUtil;
    @MockBean com.salon.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private Portfolio makePortfolio(Long id, Long profId, String profName) {
        Professional prof = new Professional();
        prof.setId(profId);
        prof.setName(profName);

        return Portfolio.builder()
                .id(id)
                .professional(prof)
                .mediaType(PortfolioMediaType.SINGLE_PHOTO)
                .photoUrl("/uploads/portfolio/test.jpg")
                .serviceTag("Haircut")
                .caption("Great result!")
                .testimonial("Loved it!")
                .isFeatured(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /portfolio → 200 with items list")
    @WithMockUser(roles = "CUSTOMER")
    void getPortfolio_returns200WithItems() throws Exception {
        when(portfolioRepository.findByProfessionalId(1L))
                .thenReturn(List.of(makePortfolio(1L, 1L, "Ram")));

        mockMvc.perform(get("/api/v1/professionals/1/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].professionalName").value("Ram"))
                .andExpect(jsonPath("$[0].serviceTag").value("Haircut"))
                .andExpect(jsonPath("$[0].mediaType").value("SINGLE_PHOTO"));
    }

    @Test
    @DisplayName("GET /portfolio → 200 with empty list when no items")
    @WithMockUser(roles = "CUSTOMER")
    void getPortfolio_returns200EmptyList() throws Exception {
        when(portfolioRepository.findByProfessionalId(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/professionals/99/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /portfolio/public → 200 without authentication")
    void getPortfolioPublic_returns200WithoutAuth() throws Exception {
        when(portfolioRepository.findByProfessionalId(1L))
                .thenReturn(List.of(makePortfolio(1L, 1L, "Ram")));

        mockMvc.perform(get("/api/v1/professionals/1/portfolio/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].professionalName").value("Ram"));
    }
}
