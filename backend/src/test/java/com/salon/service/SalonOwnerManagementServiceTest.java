package com.salon.service;

import com.salon.dto.request.SalonOwnerManagementEditRequest;
import com.salon.dto.response.SalonOwnerManagementResponse;
import com.salon.entity.SalonOwner;
import com.salon.exception.SalonOwnerNotFoundException;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.impl.SalonOwnerManagementServiceImpl;
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

@ExtendWith(MockitoExtension.class)
class SalonOwnerManagementServiceTest {

    @Mock
    private SalonOwnerRepository salonOwnerRepository;

    @InjectMocks
    private SalonOwnerManagementServiceImpl service;

    private SalonOwner existingOwner;

    @BeforeEach
    void setUp() {
        existingOwner = SalonOwner.builder()
                .id(1L)
                .name("Old Name")
                .salonName("Old Salon")
                .phone("9000000001")
                .email("owner@salon.com")
                .city("Hyderabad")
                .build();
    }

    // ── Success ───────────────────────────────────────────────────────────────

    @Test
    void updateSalonOwner_success_updatesThreeFields() {
        SalonOwnerManagementEditRequest req =
                new SalonOwnerManagementEditRequest("New Name", "New Salon", "8888888888");

        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(existingOwner));
        when(salonOwnerRepository.save(any(SalonOwner.class))).thenAnswer(inv -> inv.getArgument(0));

        SalonOwnerManagementResponse res = service.updateSalonOwner(1L, req);

        assertThat(res.getOwnerName()).isEqualTo("New Name");
        assertThat(res.getSalonName()).isEqualTo("New Salon");
        assertThat(res.getPhone()).isEqualTo("8888888888");
        // Read-only fields must be unchanged
        assertThat(res.getEmail()).isEqualTo("owner@salon.com");
        assertThat(res.getCity()).isEqualTo("Hyderabad");

        verify(salonOwnerRepository).save(any(SalonOwner.class));
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void updateSalonOwner_notFound_throwsSalonOwnerNotFoundException() {
        when(salonOwnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateSalonOwner(99L,
                        new SalonOwnerManagementEditRequest("N", "S", "9000000001")))
                .isInstanceOf(SalonOwnerNotFoundException.class)
                .hasMessageContaining("99");

        verify(salonOwnerRepository, never()).save(any());
    }

    // ── Email and city not mutated ────────────────────────────────────────────

    @Test
    void updateSalonOwner_doesNotMutateEmailOrCity() {
        SalonOwnerManagementEditRequest req =
                new SalonOwnerManagementEditRequest("X", "Y", "7777777777");

        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(existingOwner));
        when(salonOwnerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalonOwnerManagementResponse res = service.updateSalonOwner(1L, req);

        assertThat(res.getEmail()).isEqualTo("owner@salon.com");
        assertThat(res.getCity()).isEqualTo("Hyderabad");
    }
}
