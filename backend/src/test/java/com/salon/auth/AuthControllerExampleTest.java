package com.salon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Example tests for AuthController.
 *
 * Task 6.10: invalid city on professional registration returns 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerExampleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SalonOwnerRepository salonOwnerRepository;

    @BeforeEach
    void seedSalonOwners() {
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
     * Task 6.10: POST /api/auth/professional/register with an invalid city returns 400.
     */
    @Test
    void professionalRegisterWithInvalidCityReturns400() throws Exception {
        ProfessionalRegisterRequest request = ProfessionalRegisterRequest.builder()
                .name("Test Pro")
                .email("testpro@example.com")
                .password("Password123")
                .city("InvalidCity")
                .specialization("Haircut")
                .build();

        mockMvc.perform(post("/api/auth/professional/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Sanity check: valid professional registration returns 201.
     */
    @Test
    void professionalRegisterWithValidCityReturns201() throws Exception {
        ProfessionalRegisterRequest request = ProfessionalRegisterRequest.builder()
                .name("Valid Pro")
                .email("validpro@example.com")
                .password("Password123")
                .city("Hyderabad")
                .specialization("Haircut")
                .build();

        mockMvc.perform(post("/api/auth/professional/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
