package com.salon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.CustomerRegisterRequest;
import com.salon.dto.request.LoginRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test for Task 6.6: Customer register then login returns same userId and role CUSTOMER
 * 
 * **Validates: Property 1: Customer Registration Round-Trip**
 * 
 * Tests that registering a customer and then logging in with the same credentials
 * returns consistent userId and role CUSTOMER across both operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CustomerRoundTripTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanupCustomers() {
        customerRepository.deleteAll();
    }

    /**
     * Test: Customer register then login returns same userId and role CUSTOMER
     * Validates: Requirements 1.1, 1.2
     * Property: Customer Registration Round-Trip
     */
    @Test
    void customerRegisterThenLoginReturnsSameUserIdAndRoleCustomer() throws Exception {
        // Step 1: Register a customer
        CustomerRegisterRequest registerRequest = CustomerRegisterRequest.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .password("Password123")
                .city("Hyderabad")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String registerResponseBody = registerResult.getResponse().getContentAsString();
        AuthResponse registerResponse = objectMapper.readValue(registerResponseBody, AuthResponse.class);

        // Verify registration response
        assertThat(registerResponse.getToken()).isNotEmpty();
        assertThat(registerResponse.getRole()).isEqualTo("CUSTOMER");
        assertThat(registerResponse.getUserId()).isNotNull();
        assertThat(registerResponse.getName()).isEqualTo("John Doe");

        // Step 2: Login with the same credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("Password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthResponse loginResponse = objectMapper.readValue(loginResponseBody, AuthResponse.class);

        // Verify login response
        assertThat(loginResponse.getToken()).isNotEmpty();
        assertThat(loginResponse.getRole()).isEqualTo("CUSTOMER");
        assertThat(loginResponse.getUserId()).isNotNull();
        assertThat(loginResponse.getName()).isEqualTo("John Doe");

        // Step 3: Verify userId and role are consistent across both operations
        assertThat(loginResponse.getUserId()).isEqualTo(registerResponse.getUserId());
        assertThat(loginResponse.getRole()).isEqualTo(registerResponse.getRole());
    }
}
