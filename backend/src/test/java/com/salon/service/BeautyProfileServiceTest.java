package com.salon.service;

import com.salon.dto.request.BeautyProfileRequest;
import com.salon.dto.response.BeautyProfileResponse;
import com.salon.entity.BeautyProfile;
import com.salon.entity.Customer;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.BeautyProfileRepository;
import com.salon.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Updated BeautyProfileServiceTest — aligned with the current BeautyProfileService
 * which uses upsert logic and returns an empty response (not 404) when no profile exists.
 */
@ExtendWith(MockitoExtension.class)
class BeautyProfileServiceTest {

    @Mock private BeautyProfileRepository beautyProfileRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private BeautyProfileService beautyProfileService;

    private Customer customer;
    private BeautyProfileRequest request;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).name("Alice").email("alice@gmail.com").build();

        request = new BeautyProfileRequest();
        request.setSkinType("Oily");
        request.setHairType("Curly");
        request.setHairTexture("Thick");
        request.setAllergies("None");
        request.setPreferredServices("Facial, Hair Spa");
        request.setNotes("Prefer organic products");
    }

    // ── saveProfile (create) ──────────────────────────────────────────────────

    @Test
    void saveProfile_NewProfile_ShouldCreateAndReturn() {
        BeautyProfile saved = BeautyProfile.builder()
                .id(1L).customer(customer)
                .skinType("Oily").hairType("Curly").hairTexture("Thick")
                .allergies("None").preferredServices("Facial, Hair Spa")
                .notes("Prefer organic products").build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.empty());
        when(beautyProfileRepository.save(any(BeautyProfile.class))).thenReturn(saved);

        BeautyProfileResponse response = beautyProfileService.saveProfile(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getCustomerId());
        assertEquals("Oily", response.getSkinType());
        assertEquals("Curly", response.getHairType());
        assertEquals("Thick", response.getHairTexture());
        assertEquals("None", response.getAllergies());
        assertEquals("Facial, Hair Spa", response.getPreferredServices());
        verify(beautyProfileRepository).save(any(BeautyProfile.class));
    }

    @Test
    void saveProfile_ExistingProfile_ShouldUpdateInPlace() {
        BeautyProfile existing = BeautyProfile.builder()
                .id(1L).customer(customer)
                .skinType("Dry").hairType("Straight").build();

        BeautyProfile updated = BeautyProfile.builder()
                .id(1L).customer(customer)
                .skinType("Oily").hairType("Curly").hairTexture("Thick")
                .allergies("None").preferredServices("Facial, Hair Spa")
                .notes("Prefer organic products").build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.of(existing));
        when(beautyProfileRepository.save(any(BeautyProfile.class))).thenReturn(updated);

        BeautyProfileResponse response = beautyProfileService.saveProfile(1L, request);

        assertEquals("Oily", response.getSkinType());
        assertEquals("Curly", response.getHairType());
        // Verify save was called once (upsert, not insert+update)
        verify(beautyProfileRepository, times(1)).save(any(BeautyProfile.class));
    }

    @Test
    void saveProfile_CustomerNotFound_ShouldThrowResourceNotFoundException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> beautyProfileService.saveProfile(99L, request));
        verify(beautyProfileRepository, never()).save(any());
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    void getProfile_Exists_ShouldReturnProfile() {
        BeautyProfile profile = BeautyProfile.builder()
                .id(1L).customer(customer)
                .skinType("Oily").hairType("Curly").build();

        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.of(profile));

        BeautyProfileResponse response = beautyProfileService.getProfile(1L);

        assertNotNull(response);
        assertEquals(1L, response.getCustomerId());
        assertEquals("Oily", response.getSkinType());
    }

    @Test
    void getProfile_NotExists_ShouldReturnEmptyResponseWithCustomerId() {
        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        BeautyProfileResponse response = beautyProfileService.getProfile(1L);

        // Should NOT throw — returns empty response
        assertNotNull(response);
        assertEquals(1L, response.getCustomerId());
        assertNull(response.getSkinType());
        assertNull(response.getHairType());
    }

    @Test
    void getProfile_AllFieldsMapped_ShouldReturnComplete() {
        BeautyProfile profile = BeautyProfile.builder()
                .id(5L).customer(customer)
                .skinType("Normal").hairType("Wavy").hairTexture("Fine")
                .allergies("Sulfates").preferredServices("Keratin")
                .notes("Sensitive scalp").build();

        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.of(profile));

        BeautyProfileResponse response = beautyProfileService.getProfile(1L);

        assertEquals(5L, response.getId());
        assertEquals("Normal", response.getSkinType());
        assertEquals("Wavy", response.getHairType());
        assertEquals("Fine", response.getHairTexture());
        assertEquals("Sulfates", response.getAllergies());
        assertEquals("Keratin", response.getPreferredServices());
        assertEquals("Sensitive scalp", response.getNotes());
    }
}
