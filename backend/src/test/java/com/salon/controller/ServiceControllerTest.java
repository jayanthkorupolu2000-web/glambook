package com.salon.controller;

import com.salon.dto.response.ServiceResponse;
import com.salon.service.ServiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ServiceController.class)
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceService serviceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllServices_ShouldReturnGroupedServices() throws Exception {
        // Arrange
        ServiceResponse menHaircut = ServiceResponse.builder()
                .id(1L)
                .name("Haircut")
                .category("Hair")
                .gender("MEN")
                .price(new BigDecimal("150.00"))
                .durationMins(30)
                .build();

        ServiceResponse womenFacial = ServiceResponse.builder()
                .id(2L)
                .name("Facial")
                .category("Skin")
                .gender("WOMEN")
                .price(new BigDecimal("500.00"))
                .durationMins(60)
                .build();

        ServiceResponse kidsHaircut = ServiceResponse.builder()
                .id(3L)
                .name("Kids Haircut")
                .category("Hair")
                .gender("KIDS")
                .price(new BigDecimal("100.00"))
                .durationMins(20)
                .build();

        Map<String, Map<String, List<ServiceResponse>>> groupedServices = Map.of(
                "MEN", Map.of("Hair", List.of(menHaircut)),
                "WOMEN", Map.of("Skin", List.of(womenFacial)),
                "KIDS", Map.of("Hair", List.of(kidsHaircut))
        );

        when(serviceService.getAllServicesGrouped()).thenReturn(groupedServices);

        // Act & Assert
        mockMvc.perform(get("/api/services")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.MEN.Hair[0].id").value(1))
                .andExpect(jsonPath("$.MEN.Hair[0].name").value("Haircut"))
                .andExpect(jsonPath("$.MEN.Hair[0].category").value("Hair"))
                .andExpect(jsonPath("$.MEN.Hair[0].gender").value("MEN"))
                .andExpect(jsonPath("$.MEN.Hair[0].price").value(150.00))
                .andExpect(jsonPath("$.MEN.Hair[0].durationMins").value(30))
                .andExpect(jsonPath("$.WOMEN.Skin[0].id").value(2))
                .andExpect(jsonPath("$.WOMEN.Skin[0].name").value("Facial"))
                .andExpect(jsonPath("$.KIDS.Hair[0].id").value(3))
                .andExpect(jsonPath("$.KIDS.Hair[0].name").value("Kids Haircut"));
    }

    @Test
    void getAllServices_ShouldReturnEmptyMapWhenNoServices() throws Exception {
        // Arrange
        Map<String, Map<String, List<ServiceResponse>>> emptyServices = Map.of();
        when(serviceService.getAllServicesGrouped()).thenReturn(emptyServices);

        // Act & Assert
        mockMvc.perform(get("/api/services")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllServices_UnauthenticatedRequest_ShouldReturn200WithServiceList() throws Exception {
        // Arrange
        ServiceResponse menHaircut = ServiceResponse.builder()
                .id(1L)
                .name("Haircut")
                .category("Hair")
                .gender("MEN")
                .price(new BigDecimal("150.00"))
                .durationMins(30)
                .build();

        ServiceResponse womenFacial = ServiceResponse.builder()
                .id(2L)
                .name("Facial")
                .category("Skin")
                .gender("WOMEN")
                .price(new BigDecimal("500.00"))
                .durationMins(60)
                .build();

        Map<String, Map<String, List<ServiceResponse>>> groupedServices = Map.of(
                "MEN", Map.of("Hair", List.of(menHaircut)),
                "WOMEN", Map.of("Skin", List.of(womenFacial))
        );

        when(serviceService.getAllServicesGrouped()).thenReturn(groupedServices);

        // Act & Assert - Make request without Authorization header to verify public access
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.MEN.Hair[0].name").value("Haircut"))
                .andExpect(jsonPath("$.WOMEN.Skin[0].name").value("Facial"));
    }
}