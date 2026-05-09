package com.salon.service;

import com.salon.dto.request.SalonPolicyRequest;
import com.salon.dto.response.SalonPolicyResponse;
import com.salon.entity.*;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import com.salon.service.impl.SalonPolicyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonPolicyServiceTest {

    @Mock SalonPolicyRepository policyRepository;
    @Mock SalonOwnerRepository salonOwnerRepository;
    @Mock ProfessionalRepository professionalRepository;
    @Mock ProfessionalNotificationRepository notificationRepository;

    @InjectMocks SalonPolicyServiceImpl policyService;

    private SalonOwner owner;
    private SalonPolicyRequest validRequest;

    @BeforeEach
    void setUp() {
        owner = new SalonOwner();
        owner.setId(1L);
        owner.setName("Ravi Kumar");
        owner.setSalonName("Ravi Salon");
        owner.setCity("Hyderabad");

        validRequest = new SalonPolicyRequest();
        validRequest.setOwnerId(1L);
        validRequest.setTitle("No-Show Policy");
        validRequest.setContent("Customers who do not show up for their appointment without cancelling 24 hours in advance will be charged.");
    }

    // ── publishPolicy_success ─────────────────────────────────────────────────

    @Test
    @DisplayName("publishPolicy: saves policy and notifies professionals")
    void publishPolicy_success() {
        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(owner));

        SalonPolicy saved = SalonPolicy.builder()
                .id(10L).owner(owner)
                .title(validRequest.getTitle())
                .content(validRequest.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        when(policyRepository.save(any(SalonPolicy.class))).thenReturn(saved);

        Professional pro = new Professional();
        pro.setId(5L);
        pro.setName("Ram");
        when(professionalRepository.findBySalonOwnerId(1L)).thenReturn(List.of(pro));
        when(notificationRepository.saveAll(anyList())).thenReturn(List.of());

        SalonPolicyResponse result = policyService.publishPolicy(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("No-Show Policy");
        assertThat(result.getOwnerName()).isEqualTo("Ravi Kumar");

        // Verify notification was sent
        verify(notificationRepository).saveAll(argThat(list ->
                ((List<?>) list).size() == 1));
    }

    @Test
    @DisplayName("publishPolicy: sends notifications to all professionals of the owner")
    void publishPolicy_notifiesAllProfessionals() {
        when(salonOwnerRepository.findById(1L)).thenReturn(Optional.of(owner));

        SalonPolicy saved = SalonPolicy.builder().id(10L).owner(owner)
                .title("Test").content("Content").createdAt(LocalDateTime.now()).build();
        when(policyRepository.save(any())).thenReturn(saved);

        // 3 professionals mapped to this owner
        Professional p1 = new Professional(); p1.setId(1L);
        Professional p2 = new Professional(); p2.setId(2L);
        Professional p3 = new Professional(); p3.setId(3L);
        when(professionalRepository.findBySalonOwnerId(1L)).thenReturn(List.of(p1, p2, p3));
        when(notificationRepository.saveAll(anyList())).thenReturn(List.of());

        policyService.publishPolicy(validRequest);

        verify(notificationRepository).saveAll(argThat(list ->
                ((List<?>) list).size() == 3));
    }

    // ── publishPolicy_invalidOwner ────────────────────────────────────────────

    @Test
    @DisplayName("publishPolicy: throws ResourceNotFoundException when owner not found")
    void publishPolicy_invalidOwner_throwsException() {
        when(salonOwnerRepository.findById(99L)).thenReturn(Optional.empty());
        validRequest.setOwnerId(99L);

        assertThatThrownBy(() -> policyService.publishPolicy(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Owner not found");

        verify(policyRepository, never()).save(any());
        verify(notificationRepository, never()).saveAll(any());
    }

    // ── getPoliciesByCity_success ─────────────────────────────────────────────

    @Test
    @DisplayName("getPoliciesByCity: returns policies for the given city")
    void getPoliciesByCity_success() {
        SalonPolicy p1 = SalonPolicy.builder().id(1L).owner(owner).title("Policy 1").content("Content 1").createdAt(LocalDateTime.now()).build();
        SalonPolicy p2 = SalonPolicy.builder().id(2L).owner(owner).title("Policy 2").content("Content 2").createdAt(LocalDateTime.now()).build();

        when(policyRepository.findByOwnerCityIgnoreCaseOrderByCreatedAtDesc("Hyderabad"))
                .thenReturn(List.of(p1, p2));

        List<SalonPolicyResponse> result = policyService.getPoliciesByCity("Hyderabad");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Policy 1");
    }

    @Test
    @DisplayName("getPoliciesByCity: returns empty list when no policies exist")
    void getPoliciesByCity_noResults_returnsEmpty() {
        when(policyRepository.findByOwnerCityIgnoreCaseOrderByCreatedAtDesc("Khammam"))
                .thenReturn(List.of());

        List<SalonPolicyResponse> result = policyService.getPoliciesByCity("Khammam");

        assertThat(result).isEmpty();
    }
}
