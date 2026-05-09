package com.salon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.AdminLoginRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.entity.Admin;
import com.salon.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit tests for Task 6.5: POST /api/auth/admin/login
 * 
 * Tests admin login endpoint that validates against Admin record
 * and returns JWT with role ADMIN (no registration endpoint).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdmin() {
        // Delete existing admin and create a new one with known password for testing
        adminRepository.deleteAll();
        Admin admin = Admin.builder()
                .username("admin")
                .password("$2a$10$KD98mmlZ3tkoopWhuDcKfOSp3t7BL8/O8buhlhJQUUFy17SVe82Ze") // BCrypt hash for "password"
                .build();
        adminRepository.save(admin);
    }

    /**
     * Test: Valid admin credentials return 200 OK with JWT token and role ADMIN
     * Validates: Requirements 4.1
     */
    @Test
    void adminLoginWithValidCredentialsReturns200WithJWT() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .username("admin")
                .password("password")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.name").value("admin"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        
        assertThat(authResponse.getToken()).isNotEmpty();
        assertThat(authResponse.getRole()).isEqualTo("ADMIN");
        assertThat(authResponse.getUserId()).isNotNull();
        assertThat(authResponse.getName()).isEqualTo("admin");
    }

    /**
     * Test: Invalid username returns 401 Unauthorized
     * Validates: Requirements 4.2
     */
    @Test
    void adminLoginWithInvalidUsernameReturns401() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .username("wrongadmin")
                .password("password")
                .build();

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test: Invalid password returns 401 Unauthorized
     * Validates: Requirements 4.2
     */
    @Test
    void adminLoginWithInvalidPasswordReturns401() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .username("admin")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test: Missing username returns 400 Bad Request
     * Validates: DTO validation
     */
    @Test
    void adminLoginWithMissingUsernameReturns400() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .password("password")
                .build();

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Missing password returns 400 Bad Request
     * Validates: DTO validation
     */
    @Test
    void adminLoginWithMissingPasswordReturns400() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .username("admin")
                .build();

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Empty username returns 400 Bad Request
     * Validates: DTO validation with @NotBlank
     */
    @Test
    void adminLoginWithEmptyUsernameReturns400() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .username("")
                .password("password")
                .build();

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Empty password returns 400 Bad Request
     * Validates: DTO validation with @NotBlank
     */
    @Test
    void adminLoginWithEmptyPasswordReturns400() throws Exception {
        AdminLoginRequest request = AdminLoginRequest.builder()
                .username("admin")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Password is validated using BCrypt
     * Validates: Requirements 1.5 (BCrypt hashing applies to all user types)
     */
    @Test
    void adminPasswordIsStoredAsBCryptHash() {
        Admin admin = adminRepository.findByUsername("admin").orElseThrow();
        
        // Verify password is hashed (BCrypt hashes start with $2a$ or $2b$)
        assertThat(admin.getPassword()).startsWith("$2a$");
        
        // Verify BCrypt can validate the password
        assertThat(passwordEncoder.matches("password", admin.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("wrongpassword", admin.getPassword())).isFalse();
    }
}
