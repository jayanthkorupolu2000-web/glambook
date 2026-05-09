package com.salon.owner;

import com.salon.dto.response.AppointmentResponse;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.AppointmentService;
import com.salon.service.SalonOwnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for SalonOwnerService — Property 18: staff list contains only professionals
 * with matching salonOwnerId.
 */
@ExtendWith(MockitoExtension.class)
class SalonOwnerServiceTest {

    @Mock
    private SalonOwnerRepository salonOwnerRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private SalonOwnerService salonOwnerService;

    private SalonOwner owner1;
    private SalonOwner owner2;

    @BeforeEach
    void setUp() {
        owner1 = SalonOwner.builder()
                .id(1L)
                .name("Alice")
                .salonName("Alice's Salon")
                .city("Mumbai")
                .email("alice@salon.com")
                .phone("9000000001")
                .build();

        owner2 = SalonOwner.builder()
                .id(2L)
                .name("Bob")
                .salonName("Bob's Salon")
                .city("Delhi")
                .email("bob@salon.com")
                .phone("9000000002")
                .build();
    }

    // ── Profile tests ──────────────────────────────────────────────────────────

    @Test
    void getProfile_ShouldReturnOwnerDetails() {
        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(owner1));

        var response = salonOwnerService.getProfile(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getSalonName()).isEqualTo("Alice's Salon");
        assertThat(response.getCity()).isEqualTo("Mumbai");
        assertThat(response.getEmail()).isEqualTo("alice@salon.com");
        assertThat(response.getPhone()).isEqualTo("9000000001");
    }

    @Test
    void getProfile_ShouldThrowWhenOwnerNotFound() {
        when(salonOwnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salonOwnerService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Staff list tests (Property 18) ─────────────────────────────────────────

    /**
     * Validates: Property 18 — staff list contains only professionals with matching salonOwnerId.
     * The repository query is scoped to the given owner ID, so the returned list must
     * contain only professionals whose salonOwner.id equals the requested owner ID.
     */
    @Test
    void getStaff_ShouldReturnOnlyProfessionalsMatchingOwnerId() {
        Professional p1 = Professional.builder()
                .id(10L).name("Pro One").email("p1@salon.com")
                .city("Mumbai").specialization("Hair").experienceYears(3)
                .salonOwner(owner1).build();

        Professional p2 = Professional.builder()
                .id(11L).name("Pro Two").email("p2@salon.com")
                .city("Mumbai").specialization("Nails").experienceYears(2)
                .salonOwner(owner1).build();

        when(salonOwnerRepository.existsById(1L)).thenReturn(true);
        when(professionalRepository.findBySalonOwnerId(1L)).thenReturn(List.of(p1, p2));

        List<ProfessionalResponse> staff = salonOwnerService.getStaff(1L);

        assertThat(staff).hasSize(2);
        assertThat(staff).allSatisfy(pro ->
                assertThat(pro.getSalonOwner().getId()).isEqualTo(1L));
    }

    @Test
    void getStaff_ShouldNotReturnProfessionalsFromDifferentOwner() {
        // owner2's professional should never appear in owner1's staff list
        Professional p1 = Professional.builder()
                .id(10L).name("Pro One").email("p1@salon.com")
                .city("Mumbai").specialization("Hair").experienceYears(3)
                .salonOwner(owner1).build();

        // Repository returns only owner1's professionals (scoped query)
        when(salonOwnerRepository.existsById(1L)).thenReturn(true);
        when(professionalRepository.findBySalonOwnerId(1L)).thenReturn(List.of(p1));

        List<ProfessionalResponse> staff = salonOwnerService.getStaff(1L);

        assertThat(staff).hasSize(1);
        assertThat(staff.get(0).getSalonOwner().getId()).isEqualTo(1L);
        // Verify no professional belonging to owner2 is present
        assertThat(staff).noneMatch(pro -> pro.getSalonOwner().getId().equals(2L));
    }

    @Test
    void getStaff_ShouldReturnEmptyListWhenNoStaffAssigned() {
        when(salonOwnerRepository.existsById(1L)).thenReturn(true);
        when(professionalRepository.findBySalonOwnerId(1L)).thenReturn(List.of());

        List<ProfessionalResponse> staff = salonOwnerService.getStaff(1L);

        assertThat(staff).isEmpty();
    }

    @Test
    void getStaff_ShouldThrowWhenOwnerNotFound() {
        when(salonOwnerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> salonOwnerService.getStaff(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Appointments delegation test ───────────────────────────────────────────

    @Test
    void getAppointments_ShouldDelegateToAppointmentService() {
        when(salonOwnerRepository.existsById(1L)).thenReturn(true);
        when(appointmentService.getAppointmentsBySalonOwner(1L)).thenReturn(List.of());

        List<AppointmentResponse> appointments = salonOwnerService.getAppointments(1L);

        assertThat(appointments).isNotNull();
    }
}
