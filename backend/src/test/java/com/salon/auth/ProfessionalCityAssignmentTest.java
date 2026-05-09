package com.salon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.ProfessionalRegisterRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test for Task 6.9: Professional registered with city X is assigned to SalonOwner of city X
 * 
 * **Validates: Property 4: Professional City Assignment Invariant**
 * 
 * Tests that when a professional registers with a specific city, they are automatically
 * assigned to the salon owner whose city matches the registration city.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProfessionalCityAssignmentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SalonOwnerRepository salonOwnerRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

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
     * Test: Professional registered with city X is assigned to SalonOwner of city X
     * Validates: Requirements 3.1, 3.2
     * Property: Professional City Assignment Invariant
     */
    @Test
    void professionalRegisteredWithCityXIsAssignedToSalonOwnerOfCityX() throws Exception {
        // Test data: professionals for different cities
        String[][] testCases = {
            {"Pro Vizag", "pro.vizag@example.com", "Visakhapatnam", "Haircut"},
            {"Pro Vijayawada", "pro.vijayawada@example.com", "Vijayawada", "Facial"},
            {"Pro Hyderabad", "pro.hyderabad@example.com", "Hyderabad", "Massage"},
            {"Pro Ananthapur", "pro.ananthapur@example.com", "Ananthapur", "Manicure"},
            {"Pro Khammam", "pro.khammam@example.com", "Khammam", "Pedicure"}
        };

        for (String[] testCase : testCases) {
            String name = testCase[0];
            String email = testCase[1];
            String city = testCase[2];
            String specialization = testCase[3];

            // Step 1: Find the salon owner for this city
            Optional<SalonOwner> expectedOwner = salonOwnerRepository.findAll().stream()
                    .filter(owner -> owner.getCity().equals(city))
                    .findFirst();
            assertThat(expectedOwner).isPresent();

            // Step 2: Register a professional with this city
            ProfessionalRegisterRequest request = ProfessionalRegisterRequest.builder()
                    .name(name)
                    .email(email)
                    .password("Password123")
                    .city(city)
                    .specialization(specialization)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/auth/professional/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            AuthResponse response = objectMapper.readValue(responseBody, AuthResponse.class);

            // Step 3: Verify the professional was created
            assertThat(response.getToken()).isNotEmpty();
            assertThat(response.getRole()).isEqualTo("PROFESSIONAL");
            assertThat(response.getUserId()).isNotNull();

            // Step 4: Retrieve the professional from the database
            Optional<Professional> createdProfessional = professionalRepository.findByEmail(email);
            assertThat(createdProfessional).isPresent();

            Professional professional = createdProfessional.get();

            // Step 5: Verify the professional is assigned to the correct salon owner
            assertThat(professional.getCity()).isEqualTo(city);
            assertThat(professional.getSalonOwner()).isNotNull();
            assertThat(professional.getSalonOwner().getId()).isEqualTo(expectedOwner.get().getId());
            assertThat(professional.getSalonOwner().getCity()).isEqualTo(city);
        }
    }
}
