package com.salon.service;

import com.salon.dto.response.UserStatusResponse;
import com.salon.entity.*;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import com.salon.service.impl.SuspensionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuspensionServiceTest {

    @Mock private ProfessionalRepository professionalRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ComplaintRepository complaintRepository;
    @Mock private CustomerNotificationService customerNotificationService;

    @InjectMocks private SuspensionServiceImpl suspensionService;

    private Customer customer;
    private Professional professional;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L).name("Alice").email("alice@gmail.com")
                .status(UserStatus.ACTIVE).cancelCount(0).build();

        professional = Professional.builder()
                .id(2L).name("Bob").email("bob@gmail.com")
                .status(UserStatus.ACTIVE).build();
    }

    // ── autoSuspendProfessionalIfNeeded ───────────────────────────────────────

    @Test
    void autoSuspend_BelowThreshold_ShouldNotSuspend() {
        when(complaintRepository.countByProfessionalIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(3L);

        suspensionService.autoSuspendProfessionalIfNeeded(2L);

        verify(professionalRepository, never()).save(any());
    }

    @Test
    void autoSuspend_AboveThreshold_ShouldSuspendProfessional() {
        when(complaintRepository.countByProfessionalIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(6L);
        when(professionalRepository.findById(2L)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any())).thenReturn(professional);

        suspensionService.autoSuspendProfessionalIfNeeded(2L);

        verify(professionalRepository).save(argThat(p ->
                UserStatus.SUSPENDED.equals(p.getStatus())));
    }

    @Test
    void autoSuspend_ExactlyAtThreshold_ShouldNotSuspend() {
        // threshold is > 5, so exactly 5 should NOT suspend
        when(complaintRepository.countByProfessionalIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(5L);

        suspensionService.autoSuspendProfessionalIfNeeded(2L);

        verify(professionalRepository, never()).save(any());
    }

    // ── handleAppointmentCancellation ─────────────────────────────────────────

    @Test
    void handleCancellation_NotToday_ShouldDoNothing() {
        suspensionService.handleAppointmentCancellation(1L, LocalDate.now().minusDays(1));

        verify(customerRepository, never()).findById(any());
    }

    @Test
    void handleCancellation_FirstSameDayCancellation_ShouldIncrementCount() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        suspensionService.handleAppointmentCancellation(1L, LocalDate.now());

        verify(customerRepository).save(argThat(c -> c.getCancelCount() == 1));
        // Status should still be ACTIVE after first cancellation
        assertEquals(UserStatus.ACTIVE, customer.getStatus());
    }

    @Test
    void handleCancellation_ThirdSameDayCancellation_ShouldSuspend() {
        customer.setCancelCount(2); // already has 2 cancellations
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        suspensionService.handleAppointmentCancellation(1L, LocalDate.now());

        verify(customerRepository).save(argThat(c ->
                c.getCancelCount() == 3 &&
                UserStatus.SUSPENDED.equals(c.getStatus())));
    }

    @Test
    void handleCancellation_CustomerNotFound_ShouldDoNothing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        // Should not throw — ifPresent handles missing customer gracefully
        assertDoesNotThrow(() ->
                suspensionService.handleAppointmentCancellation(99L, LocalDate.now()));
    }

    // ── updateUserStatus (CUSTOMER) ───────────────────────────────────────────

    @Test
    void updateUserStatus_SuspendCustomer_ShouldSetSuspendedAndNotify() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        doNothing().when(customerNotificationService)
                .createNotification(anyLong(), any(), any(), anyString());

        UserStatusResponse response = suspensionService.updateUserStatus(
                1L, "CUSTOMER", "SUSPENDED", "Repeated cancellations");

        assertEquals("SUSPENDED", response.getStatus());
        assertEquals("CUSTOMER", response.getUserType());
        verify(customerNotificationService).createNotification(
                eq(1L), eq(CustomerNotificationType.BOOKING_CANCELLED), isNull(), anyString());
    }

    @Test
    void updateUserStatus_ReactivateCustomer_ShouldSetActiveAndNotify() {
        customer.setStatus(UserStatus.SUSPENDED);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        doNothing().when(customerNotificationService)
                .createNotification(anyLong(), any(), any(), anyString());

        UserStatusResponse response = suspensionService.updateUserStatus(
                1L, "CUSTOMER", "ACTIVE");

        assertEquals("ACTIVE", response.getStatus());
        verify(customerNotificationService).createNotification(
                eq(1L), eq(CustomerNotificationType.BOOKING_CONFIRMED), isNull(), anyString());
    }

    @Test
    void updateUserStatus_CustomerNotFound_ShouldThrow() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> suspensionService.updateUserStatus(99L, "CUSTOMER", "SUSPENDED"));
    }

    // ── updateUserStatus (PROFESSIONAL) ──────────────────────────────────────

    @Test
    void updateUserStatus_SuspendProfessional_ShouldSetSuspended() {
        when(professionalRepository.findById(2L)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any())).thenReturn(professional);

        UserStatusResponse response = suspensionService.updateUserStatus(
                2L, "PROFESSIONAL", "SUSPENDED");

        assertEquals("SUSPENDED", response.getStatus());
        assertEquals("PROFESSIONAL", response.getUserType());
    }

    @Test
    void updateUserStatus_ProfessionalNotFound_ShouldThrow() {
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> suspensionService.updateUserStatus(99L, "PROFESSIONAL", "SUSPENDED"));
    }

    @Test
    void updateUserStatus_InvalidUserType_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> suspensionService.updateUserStatus(1L, "ADMIN", "SUSPENDED"));
    }

    @Test
    void updateUserStatus_SuspendCustomer_ShouldClearSuspensionReasonOnReactivate() {
        customer.setStatus(UserStatus.SUSPENDED);
        customer.setSuspensionReason("Old reason");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(customerNotificationService)
                .createNotification(anyLong(), any(), any(), anyString());

        suspensionService.updateUserStatus(1L, "CUSTOMER", "ACTIVE");

        verify(customerRepository).save(argThat(c -> c.getSuspensionReason() == null));
    }
}
