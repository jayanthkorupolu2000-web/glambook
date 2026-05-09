package com.salon.service;

import com.salon.dto.response.ServiceResponse;
import com.salon.entity.Gender;
import com.salon.entity.Service;
import com.salon.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ServiceService serviceService;

    private List<Service> mockServices;

    @BeforeEach
    void setUp() {
        Service menHaircut = Service.builder()
                .id(1L)
                .name("Haircut")
                .category("Hair")
                .gender(Gender.MEN)
                .price(new BigDecimal("150.00"))
                .durationMins(30)
                .build();

        Service menBeardTrim = Service.builder()
                .id(2L)
                .name("Beard Trim")
                .category("Beard")
                .gender(Gender.MEN)
                .price(new BigDecimal("100.00"))
                .durationMins(20)
                .build();

        Service womenHaircut = Service.builder()
                .id(3L)
                .name("Haircut")
                .category("Hair")
                .gender(Gender.WOMEN)
                .price(new BigDecimal("250.00"))
                .durationMins(45)
                .build();

        Service womenFacial = Service.builder()
                .id(4L)
                .name("Facial")
                .category("Skin")
                .gender(Gender.WOMEN)
                .price(new BigDecimal("500.00"))
                .durationMins(60)
                .build();

        Service kidsHaircut = Service.builder()
                .id(5L)
                .name("Kids Haircut")
                .category("Hair")
                .gender(Gender.KIDS)
                .price(new BigDecimal("100.00"))
                .durationMins(20)
                .build();

        mockServices = Arrays.asList(menHaircut, menBeardTrim, womenHaircut, womenFacial, kidsHaircut);
    }

    @Test
    void getAllServicesGrouped_ShouldReturnServicesGroupedByGenderAndCategory() {
        // Arrange
        when(serviceRepository.findAll()).thenReturn(mockServices);

        // Act
        Map<String, Map<String, List<ServiceResponse>>> result = serviceService.getAllServicesGrouped();

        // Assert
        assertThat(result).hasSize(3); // MEN, WOMEN, KIDS
        
        // Check MEN services
        assertThat(result.get("MEN")).hasSize(2); // Hair, Beard categories
        assertThat(result.get("MEN").get("Hair")).hasSize(1);
        assertThat(result.get("MEN").get("Beard")).hasSize(1);
        
        // Check WOMEN services
        assertThat(result.get("WOMEN")).hasSize(2); // Hair, Skin categories
        assertThat(result.get("WOMEN").get("Hair")).hasSize(1);
        assertThat(result.get("WOMEN").get("Skin")).hasSize(1);
        
        // Check KIDS services
        assertThat(result.get("KIDS")).hasSize(1); // Hair category only
        assertThat(result.get("KIDS").get("Hair")).hasSize(1);

        // Verify service details
        ServiceResponse menHaircutResponse = result.get("MEN").get("Hair").get(0);
        assertThat(menHaircutResponse.getId()).isEqualTo(1L);
        assertThat(menHaircutResponse.getName()).isEqualTo("Haircut");
        assertThat(menHaircutResponse.getCategory()).isEqualTo("Hair");
        assertThat(menHaircutResponse.getGender()).isEqualTo("MEN");
        assertThat(menHaircutResponse.getPrice()).isEqualTo(new BigDecimal("150.00"));
        assertThat(menHaircutResponse.getDurationMins()).isEqualTo(30);
    }

    @Test
    void getAllServicesGrouped_ShouldReturnEmptyMapWhenNoServices() {
        // Arrange
        when(serviceRepository.findAll()).thenReturn(List.of());

        // Act
        Map<String, Map<String, List<ServiceResponse>>> result = serviceService.getAllServicesGrouped();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getAllServicesGrouped_ShouldHandleSingleGenderWithMultipleCategories() {
        // Arrange
        Service menHaircut = Service.builder()
                .id(1L)
                .name("Haircut")
                .category("Hair")
                .gender(Gender.MEN)
                .price(new BigDecimal("150.00"))
                .durationMins(30)
                .build();

        Service menBeardTrim = Service.builder()
                .id(2L)
                .name("Beard Trim")
                .category("Beard")
                .gender(Gender.MEN)
                .price(new BigDecimal("100.00"))
                .durationMins(20)
                .build();

        Service menFacial = Service.builder()
                .id(3L)
                .name("Face Cleanup")
                .category("Skin")
                .gender(Gender.MEN)
                .price(new BigDecimal("300.00"))
                .durationMins(45)
                .build();

        when(serviceRepository.findAll()).thenReturn(Arrays.asList(menHaircut, menBeardTrim, menFacial));

        // Act
        Map<String, Map<String, List<ServiceResponse>>> result = serviceService.getAllServicesGrouped();

        // Assert
        assertThat(result).hasSize(1); // Only MEN
        assertThat(result.get("MEN")).hasSize(3); // Hair, Beard, Skin categories
        assertThat(result.get("MEN").get("Hair")).hasSize(1);
        assertThat(result.get("MEN").get("Beard")).hasSize(1);
        assertThat(result.get("MEN").get("Skin")).hasSize(1);
    }
}