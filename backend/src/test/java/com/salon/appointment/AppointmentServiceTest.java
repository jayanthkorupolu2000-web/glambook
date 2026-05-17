package com.salon.appointment;

import com.salon.dto.request.AppointmentRequest;
import com.salon.dto.request.UpdateAppointmentStatusRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.entity.*;
import com.salon.exception.ConflictException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.ValidationException;
import com.salon.repository.*;
import com.salon.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Updated AppointmentServiceTest — aligned with the current AppointmentService
 * implementation which uses findByProfessionalIdAndDateTimeBetween (not
 * existsByProfessionalIdAndDateTime) for conflict detection, and also checks
 * ProfessionalAvailabilityRepository for break slots.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ProfessionalAvailabilityRepository availabilityRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private AppointmentService appointmentService;

    private Customer customer;
    private Professional professional;
    private Service service;
    private SalonOwner salonOwner;
    private Appointment appointment;
    private AppointmentRequest request;

    @BeforeEach
    void setUp() {
        salonOwner = SalonOwner.builder().id(1L).name("Owner").salonName("Salon")
                .city("Hyderabad").email("owner@test.com").phone("9876543210").build();

        customer = Customer.builder().id(1L).name("Alice")
                .email("alice@test.com").city("Hyderabad").build();

        professional = Professional.builder().id(1L).name("Bob")
                .email("bob@test.com").city("Hyderabad")
                .specialization("Hair Stylist").salonOwner(salonOwner).build();

        service = Service.builder().id(1L).name("Haircut").category("Hair")
                .gender(Gender.MEN).price(new BigDecimal("150.00")).durationMins(30).build();

        LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        appointment = Appointment.builder().id(1L).customer(customer)
                .professional(professional).service(service)
                .dateTime(futureTime).status(AppointmentStatus.PENDING).build();

        request = AppointmentRequest.builder()
                .customerId(1L).professionalId(1L).serviceId(1L)
                .dateTime(futureTime).build();
    }

    // ── createAppointment ─────────────────────────────────────────────────────

    @Test
    void createAppointment_NoConflict_ShouldReturnPendingAppointment() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        // No existing appointments that day
        when(appointmentRepository.findByProfessionalIdAndDateTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        // No break slots
        when(availabilityRepository.findByProfessionalIdAndAvailDate(eq(1L), any()))
                .thenReturn(List.of());
        when(availabilityRepository.findByProfessionalIdAndAvailDateAndStartTime(
                eq(1L), any(), any()))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PENDING", response.getStatus());
        assertEquals("Alice", response.getCustomerName());
        assertEquals("Bob", response.getProfessionalName());
        assertEquals("Haircut", response.getServiceName());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void createAppointment_OverlappingAppointment_ShouldThrowConflictException() {
        // Existing confirmed appointment at the same time
        Appointment existing = Appointment.builder()
                .id(99L).professional(professional).service(service)
                .dateTime(request.getDateTime()).status(AppointmentStatus.CONFIRMED).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findByProfessionalIdAndDateTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class,
                () -> appointmentService.createAppointment(request));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointment_CustomerNotFound_ShouldThrow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.createAppointment(request));
    }

    @Test
    void createAppointment_ProfessionalNotFound_ShouldThrow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.createAppointment(request));
    }

    @Test
    void createAppointment_ServiceNotFound_ShouldThrow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional));
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.createAppointment(request));
    }

    // ── getAppointmentsByCustomer ─────────────────────────────────────────────

    @Test
    void getAppointmentsByCustomer_ShouldReturnList() {
        when(appointmentRepository.findByCustomerId(1L)).thenReturn(List.of(appointment));
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        List<AppointmentResponse> responses = appointmentService.getAppointmentsByCustomer(1L);

        assertEquals(1, responses.size());
        assertEquals("Alice", responses.get(0).getCustomerName());
    }

    @Test
    void getAppointmentsByCustomer_Empty_ShouldReturnEmptyList() {
        when(appointmentRepository.findByCustomerId(1L)).thenReturn(List.of());

        List<AppointmentResponse> responses = appointmentService.getAppointmentsByCustomer(1L);

        assertTrue(responses.isEmpty());
    }

    // ── getAppointmentsBySalonOwner ───────────────────────────────────────────

    @Test
    void getAppointmentsBySalonOwner_ShouldReturnList() {
        when(appointmentRepository.findByProfessionalSalonOwnerId(1L))
                .thenReturn(List.of(appointment));
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        List<AppointmentResponse> responses = appointmentService.getAppointmentsBySalonOwner(1L);

        assertEquals(1, responses.size());
        assertEquals("Bob", responses.get(0).getProfessionalName());
    }

    // ── getAppointmentsByProfessional ─────────────────────────────────────────

    @Test
    void getAppointmentsByProfessional_ShouldReturnList() {
        when(appointmentRepository.findByProfessionalIdOrderByDateTimeDesc(1L))
                .thenReturn(List.of(appointment));
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        List<AppointmentResponse> responses = appointmentService.getAppointmentsByProfessional(1L);

        assertEquals(1, responses.size());
    }

    // ── updateAppointmentStatus ───────────────────────────────────────────────

    @Test
    void updateStatus_PendingToConfirmed_ShouldSucceed() {
        Appointment confirmed = Appointment.builder().id(1L).customer(customer)
                .professional(professional).service(service)
                .dateTime(appointment.getDateTime()).status(AppointmentStatus.CONFIRMED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(confirmed);
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        UpdateAppointmentStatusRequest req = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.CONFIRMED).build();

        AppointmentResponse response = appointmentService.updateAppointmentStatus(1L, req);

        assertEquals("CONFIRMED", response.getStatus());
    }

    @Test
    void updateStatus_ConfirmedToCompleted_ShouldSucceed() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment completed = Appointment.builder().id(1L).customer(customer)
                .professional(professional).service(service)
                .dateTime(appointment.getDateTime()).status(AppointmentStatus.COMPLETED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(completed);
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        UpdateAppointmentStatusRequest req = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.COMPLETED).build();

        AppointmentResponse response = appointmentService.updateAppointmentStatus(1L, req);

        assertEquals("COMPLETED", response.getStatus());
    }

    @Test
    void updateStatus_PendingToCompleted_ShouldThrowValidationException() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        UpdateAppointmentStatusRequest req = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.COMPLETED).build();

        assertThrows(ValidationException.class,
                () -> appointmentService.updateAppointmentStatus(1L, req));
    }

    @Test
    void updateStatus_CompletedToPending_ShouldThrowValidationException() {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        UpdateAppointmentStatusRequest req = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.PENDING).build();

        assertThrows(ValidationException.class,
                () -> appointmentService.updateAppointmentStatus(1L, req));
    }

    @Test
    void updateStatus_CancelledAppointment_ShouldThrowValidationException() {
        appointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        UpdateAppointmentStatusRequest req = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.CONFIRMED).build();

        assertThrows(ValidationException.class,
                () -> appointmentService.updateAppointmentStatus(1L, req));
    }

    @Test
    void updateStatus_NotFound_ShouldThrow() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateAppointmentStatusRequest req = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.CONFIRMED).build();

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.updateAppointmentStatus(99L, req));
    }

    // ── cancelAppointment ─────────────────────────────────────────────────────

    @Test
    void cancelAppointment_ShouldSetCancelled() {
        Appointment cancelled = Appointment.builder().id(1L).customer(customer)
                .professional(professional).service(service)
                .dateTime(appointment.getDateTime()).status(AppointmentStatus.CANCELLED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(cancelled);
        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        AppointmentResponse response = appointmentService.cancelAppointment(1L);

        assertEquals("CANCELLED", response.getStatus());
    }

    @Test
    void cancelAppointment_NotFound_ShouldThrow() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.cancelAppointment(99L));
    }
}
