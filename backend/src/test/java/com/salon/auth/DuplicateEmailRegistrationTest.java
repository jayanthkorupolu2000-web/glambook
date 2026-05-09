package com.salon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.CustomerRegisterRequest;
import com.salon.dto.request.ProfessionalRegisterRequest;
import com.salon.entity.SalonOwner;
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
 * JUnit test for Task 6.8: Duplicate email registration returns 400
 * 
 * **Validates: Property 3: Duplicate Email Registration Rejected**
 * 
 * Tests that attempting to register with an email that already exists
 * returns 400 Bad Request for both customer and professional registration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DuplicateEmailRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SalonOwnerRepository salonOwnerRepository;

    @BeforeEach
    void seedSalonOwners() {
        // Seed salon owners for professional registration tests
        String hashedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String[][] owners = {
            {"Ravi Kumar",   "Ravi Salon",   "Visakhapatnam", "ravi@salon.com",   "9000000001"},
            {"Priya Reddy",  "Priya Salon",  "Vijayawada",    "priya@salon.com",  "9000000002"},
            {"Suresh Rao",   "Suresh Salon", "Hyderabad",     "suresh@salon.com", "9000000003"},
            {"Anita Sharma", "Anita Salon",  "Ananthapur",    "anita@salon.com",  "9000000004"},
            {"Kiran Babu",   "Kiran Salon",  "Khammam",       "kiran@salon.com",  "9000000005"}
        };
        for (String[] o : owners) {
            if (salonOwnerRepository.findByEmail(o[3]).isEmpty()) {
                salonOwnerRepository.save(SalonOwner.builder()
                        .name(o[0]).salonName(o[1]).city(o[2])
                        .email(o[3]).password(hashedPassword).phone(o[4])
                        .build());
            }
        }
    }

    /**
     * Test: Duplicate customer email registration returns 400 Bad Request
     * Validates: Requirements 1.4
     * Property: Duplicate Email Registration Rejected
     */
    @Test
    void duplicateCustomerEmailReturns400() throws Exception {
        String duplicateEmail = "customer@example.com";

        // First registration - should succeed
        CustomerRegisterRequest firstRequest = CustomerRegisterRequest.builder()
                .name("First Customer")
                .email(duplicateEmail)
                .password("Password123")
                .city("Hyderabad")
                .build();

        mockMvc.perform(post("/api/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Second registration with same email - should return 400
        CustomerRegisterRequest secondRequest = CustomerRegisterRequest.builder()
                .name("Second Customer")
                .email(duplicateEmail)
                .password("DifferentPassword456")
                .city("Vijayawada")
                .build();

        mockMvc.perform(post("/api/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Duplicate professional email registration returns 400 Bad Request
     * Validates: Requirements 1.4 (applies to all user types)
     * Property: Duplicate Email Registration Rejected
     */
    @Test
    void duplicateProfessionalEmailReturns400() throws Exception {
        String duplicateEmail = "professional@example.com";

        // First registration - should succeed
        ProfessionalRegisterRequest firstRequest = ProfessionalRegisterRequest.builder()
                .name("First Professional")
                .email(duplicateEmail)
                .password("Password123")
                .city("Hyderabad")
                .specialization("Haircut")
                .build();

        mockMvc.perform(post("/api/auth/professional/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Second registration with same email - should return 400
        ProfessionalRegisterRequest secondRequest = ProfessionalRegisterRequest.builder()
                .name("Second Professional")
                .email(duplicateEmail)
                .password("DifferentPassword456")
                .city("Vijayawada")
                .specialization("Beard Trim")
                .build();

        mockMvc.perform(post("/api/auth/professional/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest());
    }
}
