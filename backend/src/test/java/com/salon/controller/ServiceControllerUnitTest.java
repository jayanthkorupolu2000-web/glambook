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
class ServiceControllerUnitTest {

    @Mock
    private ServiceService serviceService;

    @InjectMocks
    private ServiceController serviceController;

    @Test
    void getAllServices_ShouldReturnGroupedServices() {
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

        // Act
        ResponseEntity<Map<String, Map<String, List<ServiceResponse>>>> response = 
                serviceController.getAllServices();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);
        
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

        // Verify KIDS services
        assertThat(response.getBody().get("KIDS")).containsKey("Hair");
        assertThat(response.getBody().get("KIDS").get("Hair")).hasSize(1);
        ServiceResponse kidsService = response.getBody().get("KIDS").get("Hair").get(0);
        assertThat(kidsService.getName()).isEqualTo("Kids Haircut");
        assertThat(kidsService.getGender()).isEqualTo("KIDS");
    }

    @Test
    void getAllServices_ShouldReturnEmptyMapWhenNoServices() {
        // Arrange
        Map<String, Map<String, List<ServiceResponse>>> emptyServices = Map.of();
        when(serviceService.getAllServicesGrouped()).thenReturn(emptyServices);

        // Act
        ResponseEntity<Map<String, Map<String, List<ServiceResponse>>>> response = 
                serviceController.getAllServices();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAllServices_ShouldReturnCompleteServiceStructure() {
        // Arrange - Create a comprehensive service structure
        ServiceResponse menHaircut = ServiceResponse.builder()
                .id(1L).name("Haircut").category("Hair").gender("MEN")
                .price(new BigDecimal("150.00")).durationMins(30).build();
        
        ServiceResponse menBeardTrim = ServiceResponse.builder()
                .id(2L).name("Beard Trim").category("Beard").gender("MEN")
                .price(new BigDecimal("100.00")).durationMins(20).build();

        ServiceResponse womenHaircut = ServiceResponse.builder()
                .id(3L).name("Haircut").category("Hair").gender("WOMEN")
                .price(new BigDecimal("250.00")).durationMins(45).build();

        ServiceResponse womenManicure = ServiceResponse.builder()
                .id(4L).name("Manicure").category("Nails").gender("WOMEN")
                .price(new BigDecimal("300.00")).durationMins(45).build();

        ServiceResponse kidsHaircut = ServiceResponse.builder()
                .id(5L).name("Kids Haircut").category("Hair").gender("KIDS")
                .price(new BigDecimal("100.00")).durationMins(20).build();

        Map<String, Map<String, List<ServiceResponse>>> groupedServices = Map.of(
                "MEN", Map.of(
                        "Hair", List.of(menHaircut),
                        "Beard", List.of(menBeardTrim)
                ),
                "WOMEN", Map.of(
                        "Hair", List.of(womenHaircut),
                        "Nails", List.of(womenManicure)
                ),
                "KIDS", Map.of(
                        "Hair", List.of(kidsHaircut)
                )
        );

        when(serviceService.getAllServicesGrouped()).thenReturn(groupedServices);

        // Act
        ResponseEntity<Map<String, Map<String, List<ServiceResponse>>>> response = 
                serviceController.getAllServices();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3); // MEN, WOMEN, KIDS

        // Verify structure
        assertThat(response.getBody().get("MEN")).hasSize(2); // Hair, Beard
        assertThat(response.getBody().get("WOMEN")).hasSize(2); // Hair, Nails
        assertThat(response.getBody().get("KIDS")).hasSize(1); // Hair only

        // Verify specific services exist
        assertThat(response.getBody().get("MEN").get("Hair").get(0).getName()).isEqualTo("Haircut");
        assertThat(response.getBody().get("MEN").get("Beard").get(0).getName()).isEqualTo("Beard Trim");
        assertThat(response.getBody().get("WOMEN").get("Nails").get(0).getName()).isEqualTo("Manicure");
        assertThat(response.getBody().get("KIDS").get("Hair").get(0).getName()).isEqualTo("Kids Haircut");
    }
}