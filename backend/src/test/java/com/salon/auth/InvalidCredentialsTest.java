package com.salon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.AdminLoginRequest;
import com.salon.dto.request.LoginRequest;
import com.salon.repository.AdminRepository;
import com.salon.repository.CustomerRepository;
import com.salon.repository.SalonOwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test for Task 6.7: Unregistered email/password returns 401 on login
 * 
 * **Validates: Property 2: Invalid Credentials Rejected**
 * 
 * Tests that attempting to login with credentials that were never registered
 * returns 401 Unauthorized for customer, owner, and admin login endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InvalidCredentialsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SalonOwnerRepository salonOwnerRepository;

    @Autowired
    private AdminRepository adminRepository;

    @BeforeEach
    void cleanupDatabase() {
        // Ensure no matching records exist for our test credentials
        customerRepository.deleteAll();
        salonOwnerRepository.deleteAll();
        adminRepository.deleteAll();
    }

    /**
     * Test: Unregistered customer email/password returns 401 Unauthorized
     * Validates: Requirements 1.3
     * Property: Invalid Credentials Rejected
     */
    @Test
    void unregisteredCustomerCredentialsReturn401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent.customer@example.com")
                .password("SomePassword123")
                .build();

        mockMvc.perform(post("/api/auth/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test: Unregistered owner email/password returns 401 Unauthorized
     * Validates: Requirements 2.2
     * Property: Invalid Credentials Rejected
     */
    @Test
    void unregisteredOwnerCredentialsReturn401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent.owner@example.com")
                .password("SomePassword123")
                .build();

        mockMvc.perform(post("/api/auth/owner/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test: Unregistered admin username/password returns 401 Unauthorized
     * Validates: Requirements 4.2
     * Property: Invalid Credentials Rejected
     */
    @Test
    void unregisteredAdminCredentialsReturn401() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .username("nonexistentadmin")
                .password("SomePassword123")
                .build();

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
