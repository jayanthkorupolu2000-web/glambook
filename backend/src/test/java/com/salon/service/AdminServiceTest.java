package com.salon.service;

import com.salon.entity.*;
import com.salon.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private SalonOwnerRepository salonOwnerRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks private AdminService adminService;

    private Customer customer;
    private SalonOwner owner;
    private Professional professional;

    @BeforeEach
    void setUp() {
        owner = SalonOwner.builder()
                .id(1L).name("Owner").salonName("Glamour").city("Hyderabad")
                .email("owner@gmail.com").phone("9000000001").build();

        customer = Customer.builder()
                .id(1L).name("Alice").email("alice@gmail.com")
                .city("Hyderabad").phone("9000000002")
                .status(UserStatus.ACTIVE).cancelCount(0).build();

        professional = Professional.builder()
                .id(1L).name("Bob").email("bob@gmail.com")
                .city("Hyderabad").specialization("Hair Styling")
                .salonOwner(owner).status(UserStatus.ACTIVE).build();
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Test
    void getAllUsers_ShouldReturnAllRoles() {
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(salonOwnerRepository.findAll()).thenReturn(List.of(owner));
        when(professionalRepository.findAll()).thenReturn(List.of(professional));

        Map<String, Object> result = adminService.getAllUsers();

        assertNotNull(result);
        assertEquals(3, (int) result.get("totalCount"));
        assertNotNull(result.get("customers"));
        assertNotNull(result.get("owners"));
        assertNotNull(result.get("professionals"));
    }

    @Test
    void getAllUsers_EmptyRepositories_ShouldReturnZeroCount() {
        when(customerRepository.findAll()).thenReturn(List.of());
        when(salonOwnerRepository.findAll()).thenReturn(List.of());
        when(professionalRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = adminService.getAllUsers();

        assertEquals(0, (int) result.get("totalCount"));
    }

    @Test
    void getAllUsers_MultipleUsers_ShouldReturnCorrectCount() {
        Customer c2 = Customer.builder().id(2L).name("Bob").email("bob2@gmail.com")
                .city("Hyderabad").status(UserStatus.ACTIVE).cancelCount(0).build();

        when(customerRepository.findAll()).thenReturn(List.of(customer, c2));
        when(salonOwnerRepository.findAll()).thenReturn(List.of(owner));
        when(professionalRepository.findAll()).thenReturn(List.of(professional));

        Map<String, Object> result = adminService.getAllUsers();

        assertEquals(4, (int) result.get("totalCount"));
    }

    // ── getAllOwners ──────────────────────────────────────────────────────────

    @Test
    void getAllOwners_ShouldReturnOwnerList() {
        when(salonOwnerRepository.findAll()).thenReturn(List.of(owner));

        var owners = adminService.getAllOwners();

        assertEquals(1, owners.size());
        assertEquals("Owner", owners.get(0).getName());
        assertEquals("Glamour", owners.get(0).getSalonName());
    }

    @Test
    void getAllOwners_Empty_ShouldReturnEmptyList() {
        when(salonOwnerRepository.findAll()).thenReturn(List.of());

        var owners = adminService.getAllOwners();

        assertTrue(owners.isEmpty());
    }

    // ── getReports ────────────────────────────────────────────────────────────

    @Test
    void getReports_ShouldReturnAppointmentAndPaymentCounts() {
        when(appointmentRepository.count()).thenReturn(10L);
        when(paymentRepository.count()).thenReturn(7L);

        Map<String, Object> report = adminService.getReports();

        assertEquals(10L, report.get("totalAppointments"));
        assertEquals(7L, report.get("totalPayments"));
    }

    @Test
    void getReports_ZeroCounts_ShouldReturnZeros() {
        when(appointmentRepository.count()).thenReturn(0L);
        when(paymentRepository.count()).thenReturn(0L);

        Map<String, Object> report = adminService.getReports();

        assertEquals(0L, report.get("totalAppointments"));
        assertEquals(0L, report.get("totalPayments"));
    }

    // ── status mapping ────────────────────────────────────────────────────────

    @Test
    void getAllUsers_SuspendedCustomer_ShouldMapStatusCorrectly() {
        customer.setStatus(UserStatus.SUSPENDED);
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(salonOwnerRepository.findAll()).thenReturn(List.of());
        when(professionalRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = adminService.getAllUsers();

        @SuppressWarnings("unchecked")
        var customers = (List<com.salon.dto.response.CustomerResponse>) result.get("customers");
        assertEquals("SUSPENDED", customers.get(0).getStatus());
    }

    @Test
    void getAllUsers_NullStatus_ShouldDefaultToActive() {
        customer.setStatus(null);
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(salonOwnerRepository.findAll()).thenReturn(List.of());
        when(professionalRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = adminService.getAllUsers();

        @SuppressWarnings("unchecked")
        var customers = (List<com.salon.dto.response.CustomerResponse>) result.get("customers");
        assertEquals("ACTIVE", customers.get(0).getStatus());
    }
}
