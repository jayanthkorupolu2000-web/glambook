package com.salon.controller;

import com.salon.dto.response.ServiceResponse;
import com.salon.service.ServiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceControllerUnauthenticatedTest {

    @Mock
    private ServiceService serviceService;

    @InjectMocks
    private ServiceController serviceController;

    @Test
    void getAllServices_UnauthenticatedRequest_ShouldReturn200WithServiceList() {
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

        // Act - Call controller method directly (simulating unauthenticated request)
        ResponseEntity<Map<String, Map<String, List<ServiceResponse>>>> response = 
                serviceController.getAllServices();

        // Assert - Verify 200 status and service list is returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        
        // Verify MEN services
        assertThat(response.getBody().get("MEN")).containsKey("Hair");
        assertThat(response.getBody().get("MEN").get("Hair")).hasSize(1);
        ServiceResponse menService = response.getBody().get("MEN").get("Hair").get(0);
        assertThat(menService.getName()).isEqualTo("Haircut");
        assertThat(menService.getGender()).isEqualTo("MEN");
        assertThat(menService.getPrice()).isEqualTo(new BigDecimal("150.00"));

        // Verify WOMEN services
        assertThat(response.getBody().get("WOMEN")).containsKey("Skin");
        assertThat(response.getBody().get("WOMEN").get("Skin")).hasSize(1);
        ServiceResponse womenService = response.getBody().get("WOMEN").get("Skin").get(0);
        assertThat(womenService.getName()).isEqualTo("Facial");
        assertThat(womenService.getGender()).isEqualTo("WOMEN");
        assertThat(womenService.getPrice()).isEqualTo(new BigDecimal("500.00"));
    }
}