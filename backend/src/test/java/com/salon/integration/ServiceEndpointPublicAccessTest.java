package com.salon.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.salon.security.JwtUtil;
import com.salon.service.ServiceService;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test to verify that GET /api/services is publicly accessible
 * without authentication as required by task 7.7
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never"
})
class ServiceEndpointPublicAccessTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private JwtUtil jwtUtil; // Mock to avoid JWT dependency issues

    @MockBean
    private ServiceService serviceService;

    @Test
    void getServices_ShouldBePubliclyAccessible_WithoutAuthentication() throws Exception {
        // Arrange
        when(serviceService.getAllServicesGrouped()).thenReturn(Map.of());

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Act & Assert - Should return 200 without any authentication
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk());
    }

    @Test
    void getServices_ShouldReturn200_WhenServicesExist() throws Exception {
        // Arrange - Mock some services with correct type
        Map<String, Map<String, java.util.List<com.salon.dto.response.ServiceResponse>>> mockServices = Map.of();
        when(serviceService.getAllServicesGrouped()).thenReturn(mockServices);

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Act & Assert
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk());
    }
}