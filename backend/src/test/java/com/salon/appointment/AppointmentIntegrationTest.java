package com.salon.appointment;

import com.salon.dto.request.AppointmentRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.entity.*;
import com.salon.repository.*;
import com.salon.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentIntegrationTest {

    @Autowired private AppointmentService appointmentService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private SalonOwnerRepository salonOwnerRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    private Customer testCustomer;
    private Professional testProfessional;
    private Service testService;

    @BeforeEach
    void setUp() {
        SalonOwner salonOwner = SalonOwner.builder().name("Test Owner").salonName("Test Salon")
                .city("Hyderabad").email("owner@test.com").password("password").phone("9876543210").build();
        salonOwner = salonOwnerRepository.save(salonOwner);

        testCustomer = Customer.builder().name("Test Customer").email("customer@test.com")
                .password("password").city("Hyderabad").build();
        testCustomer = customerRepository.save(testCustomer);

        testProfessional = Professional.builder().name("Test Professional").email("professional@test.com")
                .password("password").city("Hyderabad").specialization("Hair Stylist").salonOwner(salonOwner).build();
        testProfessional = professionalRepository.save(testProfessional);

        testService = Service.builder().name("Haircut").category("Hair").gender(Gender.MEN)
                .price(new BigDecimal("150.00")).durationMins(30).build();
        testService = serviceRepository.save(testService);
    }

    @Test
    void createAppointment_ValidRequest_ShouldCreateAppointmentWithPendingStatus() {
        AppointmentRequest request = AppointmentRequest.builder()
                .customerId(testCustomer.getId()).professionalId(testProfessional.getId())
                .serviceId(testService.getId()).dateTime(LocalDateTime.now().plusDays(1)).build();

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(testCustomer.getName(), response.getCustomerName());
        assertEquals(testProfessional.getName(), response.getProfessionalName());
        assertEquals(testService.getName(), response.getServiceName());
        assertTrue(appointmentRepository.findById(response.getId()).isPresent());
    }

    @Test
    void getAppointmentsByCustomer_ShouldReturnCustomerAppointments() {
        AppointmentRequest request = AppointmentRequest.builder()
                .customerId(testCustomer.getId()).professionalId(testProfessional.getId())
                .serviceId(testService.getId()).dateTime(LocalDateTime.now().plusDays(1)).build();
        AppointmentResponse created = appointmentService.createAppointment(request);

        List<AppointmentResponse> appointments = appointmentService.getAppointmentsByCustomer(testCustomer.getId());

        assertNotNull(appointments);
        assertEquals(1, appointments.size());
        assertEquals(created.getId(), appointments.get(0).getId());
        assertEquals(testCustomer.getName(), appointments.get(0).getCustomerName());
    }

    @Test
    void getAppointmentsBySalonOwner_ShouldReturnSalonAppointments() {
        AppointmentRequest request = AppointmentRequest.builder()
                .customerId(testCustomer.getId()).professionalId(testProfessional.getId())
                .serviceId(testService.getId()).dateTime(LocalDateTime.now().plusDays(1)).build();
        AppointmentResponse created = appointmentService.createAppointment(request);

        List<AppointmentResponse> appointments = appointmentService.getAppointmentsBySalonOwner(testProfessional.getSalonOwner().getId());

        assertNotNull(appointments);
        assertEquals(1, appointments.size());
        assertEquals(created.getId(), appointments.get(0).getId());
        assertEquals(testProfessional.getName(), appointments.get(0).getProfessionalName());
    }

    @Test
    void cancelAppointment_ShouldSetStatusToCancelled() {
        AppointmentRequest request = AppointmentRequest.builder()
                .customerId(testCustomer.getId()).professionalId(testProfessional.getId())
                .serviceId(testService.getId()).dateTime(LocalDateTime.now().plusDays(1)).build();
        AppointmentResponse created = appointmentService.createAppointment(request);

        AppointmentResponse cancelled = appointmentService.cancelAppointment(created.getId());

        assertNotNull(cancelled);
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(created.getId(), cancelled.getId());
    }
}
