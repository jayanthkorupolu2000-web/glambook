package com.salon.professional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.dto.request.UpdateProfileRequest;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.entity.Service;
import com.salon.entity.Gender;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.repository.ServiceRepository;
import com.salon.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProfessionalController GET /api/professionals endpoint.
 * Task 7.1: Filter professionals by city with pagination.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProfessionalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private SalonOwnerRepository salonOwnerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private SalonOwner hyderabadOwner;
    private SalonOwner vijayawadaOwner;
    private Professional johnDoe;
    private Professional janeSmith;

    @BeforeEach
    void setup() {
        String hashedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        hyderabadOwner = salonOwnerRepository.save(SalonOwner.builder()
                .name("Suresh Rao")
                .salonName("Suresh Salon")
                .city("Hyderabad")
                .email("suresh@salon.com")
                .password(hashedPassword)
                .phone("9000000003")
                .build());

        vijayawadaOwner = salonOwnerRepository.save(SalonOwner.builder()
                .name("Priya Reddy")
                .salonName("Priya Salon")
                .city("Vijayawada")
                .email("priya@salon.com")
                .password(hashedPassword)
                .phone("9000000002")
                .build());

        johnDoe = professionalRepository.save(Professional.builder()
                .name("John Doe")
                .email("john@example.com")
                .password(hashedPassword)
                .city("Hyderabad")
                .specialization("Haircut")
                .experienceYears(5)
                .salonOwner(hyderabadOwner)
                .build());

        janeSmith = professionalRepository.save(Professional.builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .password(hashedPassword)
                .city("Hyderabad")
                .specialization("Facial")
                .experienceYears(3)
                .salonOwner(hyderabadOwner)
                .build());

        professionalRepository.save(Professional.builder()
                .name("Bob Wilson")
                .email("bob@example.com")
                .password(hashedPassword)
                .city("Vijayawada")
                .specialization("Massage")
                .experienceYears(7)
                .salonOwner(vijayawadaOwner)
                .build());

        serviceRepository.save(Service.builder()
                .name("Haircut")
                .category("Hair")
                .gender(Gender.MEN)
                .price(new BigDecimal("150.00"))
                .durationMins(30)
                .build());
    }

    @Test
    void getProfessionalsByCity_returnsFilteredProfessionals() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Hyderabad")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].city", everyItem(is("Hyderabad"))))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("John Doe", "Jane Smith")));
    }

    @Test
    void getProfessionalsByCity_returnsCorrectFields() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Hyderabad")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").exists())
                .andExpect(jsonPath("$.content[0].city").exists())
                .andExpect(jsonPath("$.content[0].specialization").exists())
                .andExpect(jsonPath("$.content[0].salonOwner").exists())
                .andExpect(jsonPath("$.content[0].salonOwner.salonName").exists())
                .andExpect(jsonPath("$.content[0].services").isArray())
                .andExpect(jsonPath("$.content[0].rating").value(nullValue()));
    }

    @Test
    void getProfessionalsByCity_respectsPagination() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Hyderabad")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.size", is(1)));
    }

    @Test
    void getProfessionalsByCity_emptyResultForNoMatches() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Khammam")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void getProfessionalsByCity_defaultPaginationValues() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    void getProfessionalsByCity_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Vijayawada")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Bob Wilson")));
    }

    @Test
    void getProfessionalById_returnsFullProfile() throws Exception {
        Professional professional = professionalRepository.findAll().stream()
                .filter(p -> p.getName().equals("John Doe"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/professionals/{id}", professional.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(professional.getId().intValue())))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")))
                .andExpect(jsonPath("$.city", is("Hyderabad")))
                .andExpect(jsonPath("$.specialization", is("Haircut")))
                .andExpect(jsonPath("$.experienceYears", is(5)))
                .andExpect(jsonPath("$.salonOwner").exists())
                .andExpect(jsonPath("$.salonOwner.salonName", is("Suresh Salon")))
                .andExpect(jsonPath("$.services").isArray())
                .andExpect(jsonPath("$.rating").value(nullValue()));
    }

    @Test
    void getProfessionalById_throwsNotFoundForInvalidId() throws Exception {
        mockMvc.perform(get("/api/professionals/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfessionalById_isPubliclyAccessible() throws Exception {
        Professional professional = professionalRepository.findAll().stream()
                .filter(p -> p.getName().equals("Jane Smith"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/professionals/{id}", professional.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Jane Smith")));
    }

    @Test
    void updateProfile_successfullyUpdatesOwnProfile() throws Exception {
        String token = jwtUtil.generateToken("john@example.com", "PROFESSIONAL", johnDoe.getId(), "Hyderabad");
        
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("John Updated")
                .specialization("Advanced Haircut")
                .experienceYears(6)
                .build();

        mockMvc.perform(put("/api/professionals/{id}/profile", johnDoe.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("John Updated")))
                .andExpect(jsonPath("$.specialization", is("Advanced Haircut")))
                .andExpect(jsonPath("$.experienceYears", is(6)));
    }

    @Test
    void updateProfile_partialUpdateSucceeds() throws Exception {
        String token = jwtUtil.generateToken("john@example.com", "PROFESSIONAL", johnDoe.getId(), "Hyderabad");
        
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("John Partial")
                .build();

        mockMvc.perform(put("/api/professionals/{id}/profile", johnDoe.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("John Partial")))
                .andExpect(jsonPath("$.specialization", is("Haircut"))) // unchanged
                .andExpect(jsonPath("$.experienceYears", is(5))); // unchanged
    }

    @Test
    void updateProfile_requiresAuthentication() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("John Updated")
                .build();

        mockMvc.perform(put("/api/professionals/{id}/profile", johnDoe.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Spring Security returns 403 for unauthenticated requests to protected endpoints
    }

    @Test
    void updateProfile_requiresProfessionalRole() throws Exception {
        String token = jwtUtil.generateToken("customer@example.com", "CUSTOMER", 999L, "Hyderabad");
        
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("John Updated")
                .build();

        mockMvc.perform(put("/api/professionals/{id}/profile", johnDoe.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProfile_cannotUpdateOtherProfessionalProfile() throws Exception {
        String token = jwtUtil.generateToken("jane@example.com", "PROFESSIONAL", janeSmith.getId(), "Hyderabad");
        
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("John Hacked")
                .build();

        mockMvc.perform(put("/api/professionals/{id}/profile", johnDoe.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_returnsNotFoundForInvalidId() throws Exception {
        String token = jwtUtil.generateToken("john@example.com", "PROFESSIONAL", 99999L, "Hyderabad");
        
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("John Updated")
                .build();

        mockMvc.perform(put("/api/professionals/{id}/profile", 99999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_validatesRequestFields() throws Exception {
        String token = jwtUtil.generateToken("john@example.com", "PROFESSIONAL", johnDoe.getId(), "Hyderabad");
        
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("J") // Too short, should fail validation
                .build();

        mockMvc.perform(put("/api/professionals/{id}/profile", johnDoe.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Task 7.5: Write JUnit test: professionals filtered by city all have matching city in response
     * Validates Property 7: City Filter Correctness
     * 
     * For any city value passed to GET /api/professionals, every professional in the 
     * returned list should have a city field equal to the requested city.
     */
    @Test
    void cityFilterCorrectness_allReturnedProfessionalsHaveMatchingCity() throws Exception {
        // Test with Hyderabad - should return 2 professionals, both with city "Hyderabad"
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Hyderabad")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].city", everyItem(is("Hyderabad"))));

        // Test with Vijayawada - should return 1 professional with city "Vijayawada"
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Vijayawada")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[*].city", everyItem(is("Vijayawada"))));

        // Test with city that has no professionals - should return empty list
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Ananthapur")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    /**
     * Task 7.6: Write JUnit test: returned list size is at most the requested page size
     * Validates Property 8: Pagination Size Invariant
     * 
     * For any page and size parameters passed to the professional browse endpoint, 
     * the number of returned professionals should be at most `size`.
     */
    @Test
    void paginationSizeInvariant_returnedListSizeIsAtMostRequestedSize() throws Exception {
        // Test with size=1 when 2 professionals exist - should return exactly 1
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Hyderabad")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.size", is(1)));

        // Test with size=5 when only 2 professionals exist - should return 2 (less than requested size)
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Hyderabad")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.size", is(5)));

        // Test with size=3 when only 1 professional exists - should return 1 (less than requested size)
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Vijayawada")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(3))))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.size", is(3)));

        // Test with large size when fewer professionals exist - should never exceed available count
        mockMvc.perform(get("/api/professionals")
                        .param("city", "Vijayawada")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(100))))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.size", is(100)));
    }
}
