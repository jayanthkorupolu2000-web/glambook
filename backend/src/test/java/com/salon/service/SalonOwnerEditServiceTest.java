package com.salon.service;

import com.salon.dto.request.SalonOwnerEditRequest;
import com.salon.dto.response.SalonOwnerEditResponse;
import com.salon.entity.SalonOwner;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.impl.SalonOwnerEditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalonOwnerEditServiceImpl.
 * Uses Mockito to mock the repository — no real DB involved.
 */
@ExtendWith(MockitoExtension.class)
class SalonOwnerEditServiceTest {

    @Mock
    private SalonOwnerRepository salonOwnerRepository;

    @InjectMocks
    private SalonOwnerEditServiceImpl service;

    private SalonOwner existingOwner;

    @BeforeEach
    void setUp() {
        existingOwner = SalonOwner.builder()
                .id(1L)
                .name("Old Name")
                .phone("9000000001")
                .email("owner@salon.com")
                .city("Hyderabad")
                .salonName("Old Salon")
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void updateSalonOwner_success_updatesNameAndPhone() {
        // Arrange
        SalonOwnerEditRequest request = new SalonOwnerEditRequest("New Name", "8888888888", "Old Salon");

        SalonOwner savedOwner = SalonOwner.builder()
                .id(1L)
                .name("New Name")
                .phone("8888888888")
                .email("owner@salon.com")
                .city("Hyderabad")
                .salonName("Old Salon")
                .build();

        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(existingOwner));
        when(salonOwnerRepository.save(any(SalonOwner.class))).thenReturn(savedOwner);

        // Act
        SalonOwnerEditResponse response = service.updateSalonOwner(1L, request);

        // Assert
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getPhone()).isEqualTo("8888888888");
        assertThat(response.getEmail()).isEqualTo("owner@salon.com");   // unchanged
        assertThat(response.getCity()).isEqualTo("Hyderabad");          // unchanged
        assertThat(response.getRole()).isEqualTo("SALON_OWNER");
        assertThat(response.getAdditionalInfo()).isEqualTo("Old Salon"); // unchanged

        verify(salonOwnerRepository).findById(1L);
        verify(salonOwnerRepository).save(any(SalonOwner.class));
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void updateSalonOwner_notFound_throwsResourceNotFoundException() {
        when(salonOwnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateSalonOwner(99L, new SalonOwnerEditRequest("Name", "9000000001", "Old Salon")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(salonOwnerRepository, never()).save(any());
    }

    // ── Email/city not mutated ────────────────────────────────────────────────

    @Test
    void updateSalonOwner_doesNotMutateEmailOrCity() {
        SalonOwnerEditRequest request = new SalonOwnerEditRequest("Updated Name", "7777777777", "Old Salon");

        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(existingOwner));
        when(salonOwnerRepository.save(any(SalonOwner.class))).thenAnswer(inv -> inv.getArgument(0));

        SalonOwnerEditResponse response = service.updateSalonOwner(1L, request);

        // Email and city must remain exactly as they were
        assertThat(response.getEmail()).isEqualTo("owner@salon.com");
        assertThat(response.getCity()).isEqualTo("Hyderabad");
    }
}
