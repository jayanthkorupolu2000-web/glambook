package com.salon.service;

import com.salon.dto.request.BeautyProfileRequest;
import com.salon.dto.response.BeautyProfileResponse;
import com.salon.entity.BeautyProfile;
import com.salon.entity.Customer;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.BeautyProfileRepository;
import com.salon.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeautyProfileServiceTest {

    @Mock BeautyProfileRepository beautyProfileRepository;
    @Mock CustomerRepository customerRepository;

    @InjectMocks BeautyProfileService beautyProfileService;

    private Customer customer;
    private BeautyProfileRequest validRequest;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setName("Test Customer");

        validRequest = new BeautyProfileRequest();
        validRequest.setSkinType("Oily");
        validRequest.setHairType("Wavy");
        validRequest.setHairTexture("Medium");
        validRequest.setAllergies("Ammonia");
        validRequest.setPreferredServices("Hair Color");
        validRequest.setNotes("No harsh chemicals");
    }

    // ── saveProfile_success ───────────────────────────────────────────────────

    @Test
    @DisplayName("saveProfile: creates new profile when none exists")
    void saveProfile_createsNew_success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.empty());
        when(beautyProfileRepository.save(any(BeautyProfile.class))).thenAnswer(inv -> {
            BeautyProfile p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        BeautyProfileResponse result = beautyProfileService.saveProfile(1L, validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getSkinType()).isEqualTo("Oily");
        assertThat(result.getHairType()).isEqualTo("Wavy");
        assertThat(result.getHairTexture()).isEqualTo("Medium");
        assertThat(result.getAllergies()).isEqualTo("Ammonia");
        verify(beautyProfileRepository).save(any(BeautyProfile.class));
    }

    @Test
    @DisplayName("saveProfile: updates existing profile")
    void saveProfile_updatesExisting_success() {
        BeautyProfile existing = BeautyProfile.builder()
                .id(10L).customer(customer).skinType("Dry").build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.of(existing));
        when(beautyProfileRepository.save(any(BeautyProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        BeautyProfileResponse result = beautyProfileService.saveProfile(1L, validRequest);

        assertThat(result.getSkinType()).isEqualTo("Oily"); // updated from Dry
        assertThat(result.getHairTexture()).isEqualTo("Medium");
        verify(beautyProfileRepository).save(existing);
    }

    // ── saveProfile_invalidData ───────────────────────────────────────────────

    @Test
    @DisplayName("saveProfile: throws ResourceNotFoundException when customer not found")
    void saveProfile_customerNotFound_throwsException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beautyProfileService.saveProfile(99L, validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");

        verify(beautyProfileRepository, never()).save(any());
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProfile: returns profile when it exists")
    void getProfile_exists_returnsProfile() {
        BeautyProfile profile = BeautyProfile.builder()
                .id(10L).customer(customer).skinType("Normal").hairType("Straight").build();

        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.of(profile));

        BeautyProfileResponse result = beautyProfileService.getProfile(1L);

        assertThat(result.getSkinType()).isEqualTo("Normal");
        assertThat(result.getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getProfile: returns empty response when no profile exists")
    void getProfile_notExists_returnsEmpty() {
        when(beautyProfileRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        BeautyProfileResponse result = beautyProfileService.getProfile(1L);

        assertThat(result).isNotNull();
        assertThat(result.getSkinType()).isNull();
        assertThat(result.getCustomerId()).isEqualTo(1L);
    }
}
