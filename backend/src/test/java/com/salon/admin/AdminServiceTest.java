package com.salon.admin;

import com.salon.dto.response.CustomerResponse;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.dto.response.SalonOwnerResponse;
import com.salon.entity.Customer;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.CustomerRepository;
import com.salon.repository.PaymentRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for AdminService — Property 19: admin user list contains all registered users
 * across all roles (customers, salon owners, professionals).
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SalonOwnerRepository salonOwnerRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminService adminService;

    private Customer customer1;
    private Customer customer2;
    private SalonOwner owner1;
    private Professional professional1;

    @BeforeEach
    void setUp() {
        customer1 = Customer.builder()
                .id(1L).name("Alice Customer").email("alice@example.com")
                .phone("9000000001").city("Hyderabad").build();

        customer2 = Customer.builder()
                .id(2L).name("Bob Customer").email("bob@example.com")
                .phone("9000000002").city("Vijayawada").build();

        owner1 = SalonOwner.builder()
                .id(10L).name("Owner One").salonName("Salon One")
                .city("Hyderabad").email("owner@salon.com").phone("9000000010").build();

        professional1 = Professional.builder()
                .id(20L).name("Pro One").email("pro@salon.com")
                .city("Hyderabad").specialization("Hair").experienceYears(3)
                .salonOwner(owner1).build();
    }

    // ── getAllUsers tests (Property 19) ────────────────────────────────────────

    /**
     * Validates: Property 19 — admin user list contains all registered users across all roles.
     * The response must include customers, owners, and professionals with correct counts.
     */
    @Test
    void getAllUsers_ShouldContainAllRoles() {
        when(customerRepository.findAll()).thenReturn(List.of(customer1, customer2));
        when(salonOwnerRepository.findAll()).thenReturn(List.of(owner1));
        when(professionalRepository.findAll()).thenReturn(List.of(professional1));

        Map<String, Object> result = adminService.getAllUsers();

        assertThat(result).containsKeys("customers", "owners", "professionals", "totalCount");

        @SuppressWarnings("unchecked")
        List<CustomerResponse> customers = (List<CustomerResponse>) result.get("customers");
        @SuppressWarnings("unchecked")
        List<SalonOwnerResponse> owners = (List<SalonOwnerResponse>) result.get("owners");
        @SuppressWarnings("unchecked")
        List<ProfessionalResponse> professionals = (List<ProfessionalResponse>) result.get("professionals");

        assertThat(customers).hasSize(2);
        assertThat(owners).hasSize(1);
        assertThat(professionals).hasSize(1);
        assertThat(result.get("totalCount")).isEqualTo(4);
    }

    @Test
    void getAllUsers_ShouldMapCustomerFieldsCorrectly() {
        when(customerRepository.findAll()).thenReturn(List.of(customer1));
        when(salonOwnerRepository.findAll()).thenReturn(List.of());
        when(professionalRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = adminService.getAllUsers();

        @SuppressWarnings("unchecked")
        List<CustomerResponse> customers = (List<CustomerResponse>) result.get("customers");
        CustomerResponse response = customers.get(0);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice Customer");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getCity()).isEqualTo("Hyderabad");
    }

    @Test
    void getAllUsers_ShouldReturnEmptyListsWhenNoUsersExist() {
        when(customerRepository.findAll()).thenReturn(List.of());
        when(salonOwnerRepository.findAll()).thenReturn(List.of());
        when(professionalRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = adminService.getAllUsers();

        assertThat(result.get("totalCount")).isEqualTo(0);
    }

    // ── getAllOwners tests ─────────────────────────────────────────────────────

    @Test
    void getAllOwners_ShouldReturnAllSalonOwners() {
        SalonOwner owner2 = SalonOwner.builder()
                .id(11L).name("Owner Two").salonName("Salon Two")
                .city("Vijayawada").email("owner2@salon.com").phone("9000000011").build();

        when(salonOwnerRepository.findAll()).thenReturn(List.of(owner1, owner2));

        List<SalonOwnerResponse> owners = adminService.getAllOwners();

        assertThat(owners).hasSize(2);
        assertThat(owners).extracting(SalonOwnerResponse::getId)
                .containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void getAllOwners_ShouldMapOwnerFieldsCorrectly() {
        when(salonOwnerRepository.findAll()).thenReturn(List.of(owner1));

        List<SalonOwnerResponse> owners = adminService.getAllOwners();

        SalonOwnerResponse response = owners.get(0);
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Owner One");
        assertThat(response.getSalonName()).isEqualTo("Salon One");
        assertThat(response.getCity()).isEqualTo("Hyderabad");
    }

    // ── getReports tests ───────────────────────────────────────────────────────

    @Test
    void getReports_ShouldReturnAppointmentAndPaymentCounts() {
        when(appointmentRepository.count()).thenReturn(15L);
        when(paymentRepository.count()).thenReturn(10L);

        Map<String, Object> report = adminService.getReports();

        assertThat(report.get("totalAppointments")).isEqualTo(15L);
        assertThat(report.get("totalPayments")).isEqualTo(10L);
    }

    @Test
    void getReports_ShouldReturnZeroCountsWhenEmpty() {
        when(appointmentRepository.count()).thenReturn(0L);
        when(paymentRepository.count()).thenReturn(0L);

        Map<String, Object> report = adminService.getReports();

        assertThat(report.get("totalAppointments")).isEqualTo(0L);
        assertThat(report.get("totalPayments")).isEqualTo(0L);
    }
}
