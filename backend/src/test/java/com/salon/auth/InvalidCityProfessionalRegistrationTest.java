package com.salon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.ProfessionalRegisterRequest;
import com.salon.dto.response.ErrorResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test for Task 6.10: Invalid city on professional registration returns 400 with correct message
 * 
 * **Validates: Requirement 3.3**
 * 
 * Tests that when a professional attempts to register with a city that has no salon owner,
 * the system returns 400 Bad Request with the message "No salon available in selected city".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InvalidCityProfessionalRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SalonOwnerRepository salonOwnerRepository;

    @BeforeEach
    void cleanupDatabase() {
        // Ensure no salon owners exist for our test city
        salonOwnerRepository.deleteAll();
    }

    /**
     * Test: Professional registration with invalid city returns 400 Bad Request
     * Validates: Requirement 3.3
     * 
     * When a professional registers with a city that has no salon owner,
     * the system should return 400 Bad Request with the message
     * "No salon available in selected city".
     */
    @Test
    void professionalRegistrationWithInvalidCityReturns400WithCorrectMessage() throws Exception {
        // Arrange: Create a registration request with a city that has no salon owner
        ProfessionalRegisterRequest request = ProfessionalRegisterRequest.builder()
                .name("Test Professional")
                .email("test.professional@example.com")
                .password("Password123")
                .city("Hyderabad")  // No salon owner exists for this city
                .specialization("Haircut")
                .build();

        // Act: Attempt to register the professional
        MvcResult result = mockMvc.perform(post("/api/auth/professional/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assert: Verify the error response contains the correct message
        String responseBody = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getMessage()).isEqualTo("No salon available in selected city");
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }
}
