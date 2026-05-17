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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for SalonOwnerEditServiceImpl.
 * The impl only mutates name and phone — email, city, salonName are read-only.
 * The response uses additionalInfo for salonName.
 */
@ExtendWith(MockitoExtension.class)
class SalonOwnerEditServiceTest {

    @Mock private SalonOwnerRepository salonOwnerRepository;

    @InjectMocks private SalonOwnerEditServiceImpl salonOwnerEditService;

    private SalonOwner owner;

    @BeforeEach
    void setUp() {
        owner = SalonOwner.builder()
                .id(1L).name("Old Name").salonName("Glamour Salon")
                .phone("9000000001").email("owner@salon.com").city("Hyderabad").build();
    }

    // ── updateSalonOwner ──────────────────────────────────────────────────────

    @Test
    void updateSalonOwner_ValidRequest_ShouldUpdateNameAndPhone() {
        SalonOwnerEditRequest req = new SalonOwnerEditRequest();
        req.setName("New Name");
        req.setPhone("8888888888");

        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(salonOwnerRepository.save(any(SalonOwner.class))).thenAnswer(i -> i.getArgument(0));

        SalonOwnerEditResponse response = salonOwnerEditService.updateSalonOwner(1L, req);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getPhone()).isEqualTo("8888888888");
        // Read-only fields must be unchanged
        assertThat(response.getEmail()).isEqualTo("owner@salon.com");
        assertThat(response.getCity()).isEqualTo("Hyderabad");
        assertThat(response.getAdditionalInfo()).isEqualTo("Glamour Salon");
        assertThat(response.getRole()).isEqualTo("SALON_OWNER");
    }

    @Test
    void updateSalonOwner_NotFound_ShouldThrowResourceNotFoundException() {
        when(salonOwnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                salonOwnerEditService.updateSalonOwner(99L, new SalonOwnerEditRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(salonOwnerRepository, never()).save(any());
    }

    @Test
    void updateSalonOwner_ShouldCallSaveExactlyOnce() {
        SalonOwnerEditRequest req = new SalonOwnerEditRequest();
        req.setName("X"); req.setPhone("9000000001");

        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(salonOwnerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        salonOwnerEditService.updateSalonOwner(1L, req);

        verify(salonOwnerRepository, times(1)).save(any(SalonOwner.class));
    }

    @Test
    void updateSalonOwner_ResponseIdMatchesOwnerId() {
        SalonOwnerEditRequest req = new SalonOwnerEditRequest();
        req.setName("Alice"); req.setPhone("9000000002");

        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(salonOwnerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SalonOwnerEditResponse response = salonOwnerEditService.updateSalonOwner(1L, req);

        assertThat(response.getId()).isEqualTo(1L);
    }
}
