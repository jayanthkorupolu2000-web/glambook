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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private AppointmentService appointmentService;

    private Customer testCustomer;
    private Professional testProfessional;
    private Service testService;
    private SalonOwner testSalonOwner;
    private Appointment testAppointment;
    private AppointmentRequest appointmentRequest;

    @BeforeEach
    void setUp() {
        testSalonOwner = SalonOwner.builder().id(1L).name("Test Owner").salonName("Test Salon")
                .city("Hyderabad").email("owner@test.com").phone("9876543210").build();
        testCustomer = Customer.builder().id(1L).name("Test Customer")
                .email("customer@test.com").city("Hyderabad").build();
        testProfessional = Professional.builder().id(1L).name("Test Professional")
                .email("professional@test.com").city("Hyderabad")
                .specialization("Hair Stylist").salonOwner(testSalonOwner).build();
        testService = Service.builder().id(1L).name("Haircut").category("Hair")
                .gender(Gender.MEN).price(new BigDecimal("150.00")).durationMins(30).build();
        testAppointment = Appointment.builder().id(1L).customer(testCustomer)
                .professional(testProfessional).service(testService)
                .dateTime(LocalDateTime.now().plusDays(1)).status(AppointmentStatus.PENDING).build();
        appointmentRequest = AppointmentRequest.builder().customerId(1L).professionalId(1L)
                .serviceId(1L).dateTime(LocalDateTime.now().plusDays(1)).build();
    }

    @Test
    void createAppointment_ValidRequest_ShouldCreateAppointmentWithPendingStatus() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(testProfessional));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(appointmentRepository.existsByProfessionalIdAndDateTime(any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        AppointmentResponse response = appointmentService.createAppointment(appointmentRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(testCustomer.getName(), response.getCustomerName());
        assertEquals(testProfessional.getName(), response.getProfessionalName());
        assertEquals(testService.getName(), response.getServiceName());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void createAppointment_TimeSlotConflict_ShouldThrowConflictException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(testProfessional));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(appointmentRepository.existsByProfessionalIdAndDateTime(1L, appointmentRequest.getDateTime())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> appointmentService.createAppointment(appointmentRequest));
        assertEquals("Time slot unavailable", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointment_CustomerNotFound_ShouldThrowResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createAppointment(appointmentRequest));
    }

    @Test
    void createAppointment_ProfessionalNotFound_ShouldThrowResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(professionalRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createAppointment(appointmentRequest));
    }

    @Test
    void createAppointment_ServiceNotFound_ShouldThrowResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(testProfessional));
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createAppointment(appointmentRequest));
    }

    @Test
    void getAppointmentsByCustomer_ShouldReturnCustomerAppointments() {
        when(appointmentRepository.findByCustomerId(1L)).thenReturn(Arrays.asList(testAppointment));
        List<AppointmentResponse> responses = appointmentService.getAppointmentsByCustomer(1L);
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testCustomer.getName(), responses.get(0).getCustomerName());
    }

    @Test
    void getAppointmentsBySalonOwner_ShouldReturnSalonAppointments() {
        when(appointmentRepository.findByProfessionalSalonOwnerId(1L)).thenReturn(Arrays.asList(testAppointment));
        List<AppointmentResponse> responses = appointmentService.getAppointmentsBySalonOwner(1L);
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testProfessional.getName(), responses.get(0).getProfessionalName());
    }

    @Test
    void updateAppointmentStatus_ValidTransition_ShouldUpdateStatus() {
        UpdateAppointmentStatusRequest request = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.CONFIRMED).build();
        Appointment updated = Appointment.builder().id(1L).customer(testCustomer)
                .professional(testProfessional).service(testService)
                .dateTime(testAppointment.getDateTime()).status(AppointmentStatus.CONFIRMED).build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any())).thenReturn(updated);

        AppointmentResponse response = appointmentService.updateAppointmentStatus(1L, request);
        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
    }

    @Test
    void updateAppointmentStatus_InvalidTransition_ShouldThrowValidationException() {
        testAppointment.setStatus(AppointmentStatus.COMPLETED);
        UpdateAppointmentStatusRequest request = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.PENDING).build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        assertThrows(ValidationException.class, () -> appointmentService.updateAppointmentStatus(1L, request));
    }

    @Test
    void cancelAppointment_ShouldSetStatusToCancelled() {
        Appointment cancelled = Appointment.builder().id(1L).customer(testCustomer)
                .professional(testProfessional).service(testService)
                .dateTime(testAppointment.getDateTime()).status(AppointmentStatus.CANCELLED).build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any())).thenReturn(cancelled);

        AppointmentResponse response = appointmentService.cancelAppointment(1L);
        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
    }

    @Test
    void cancelAppointment_AppointmentNotFound_ShouldThrowResourceNotFoundException() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> appointmentService.cancelAppointment(1L));
    }

    @Test
    void updateAppointmentStatus_CancelledAppointment_ShouldThrowValidationException() {
        testAppointment.setStatus(AppointmentStatus.CANCELLED);
        UpdateAppointmentStatusRequest request = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.CONFIRMED).build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        assertThrows(ValidationException.class, () -> appointmentService.updateAppointmentStatus(1L, request));
    }

    @Test
    void updateAppointmentStatus_AppointmentNotFound_ShouldThrowResourceNotFoundException() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());
        UpdateAppointmentStatusRequest request = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.CONFIRMED).build();
        assertThrows(ResourceNotFoundException.class, () -> appointmentService.updateAppointmentStatus(99L, request));
    }
}
