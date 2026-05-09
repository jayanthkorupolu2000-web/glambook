package com.salon.auth;

import com.salon.dto.request.AdminLoginRequest;
import com.salon.dto.request.CustomerRegisterRequest;
import com.salon.dto.request.LoginRequest;
import com.salon.dto.request.ProfessionalRegisterRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.entity.Admin;
import com.salon.entity.Customer;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.exception.UnauthorizedException;
import com.salon.exception.ValidationException;
import com.salon.repository.AdminRepository;
import com.salon.repository.CustomerRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.security.JwtUtil;
import com.salon.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService covering all register and login methods.
 * Uses Mockito to mock all dependencies (no Spring context).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private SalonOwnerRepository salonOwnerRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Customer customer;
    private Professional professional;
    private SalonOwner salonOwner;
    private Admin admin;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L).name("Alice").email("alice@example.com")
                .password("hashed_password").city("Hyderabad").build();

        salonOwner = SalonOwner.builder()
                .id(10L).name("Owner").salonName("Best Salon")
                .email("owner@example.com").password("hashed_password")
                .city("Hyderabad").phone("9000000001").build();

        professional = Professional.builder()
                .id(2L).name("Bob").email("bob@example.com")
                .password("hashed_password").city("Hyderabad")
                .specialization("Haircut").salonOwner(salonOwner).build();

        admin = Admin.builder()
                .id(99L).username("admin").password("hashed_password").build();
    }

    // ── registerCustomer ──────────────────────────────────────────────────────

    @Test
    void registerCustomer_NewEmail_ShouldReturnAuthResponseWithRoleCustomer() {
        CustomerRegisterRequest req = CustomerRegisterRequest.builder()
                .name("Alice").email("alice@example.com")
                .password("Password123").city("Hyderabad").build();

        when(customerRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("hashed_password");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(jwtUtil.generateToken(eq("alice@example.com"), eq("CUSTOMER"), eq(1L), eq("Hyderabad")))
                .thenReturn("jwt_token");

        AuthResponse response = authService.registerCustomer(req);

        assertNotNull(response);
        assertEquals("CUSTOMER", response.getRole());
        assertEquals(1L, response.getUserId());
        assertEquals("Alice", response.getName());
        assertEquals("jwt_token", response.getToken());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void registerCustomer_DuplicateEmail_ShouldThrowValidationException() {
        CustomerRegisterRequest req = CustomerRegisterRequest.builder()
                .name("Alice").email("alice@example.com")
                .password("Password123").city("Hyderabad").build();

        when(customerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.registerCustomer(req));

        assertEquals("Email already registered", ex.getMessage());
        verify(customerRepository, never()).save(any());
    }

    // ── loginCustomer ─────────────────────────────────────────────────────────

    @Test
    void loginCustomer_ValidCredentials_ShouldReturnAuthResponseWithRoleCustomer() {
        LoginRequest req = LoginRequest.builder()
                .email("alice@example.com").password("Password123").build();

        when(customerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(eq("alice@example.com"), eq("CUSTOMER"), eq(1L), eq("Hyderabad")))
                .thenReturn("jwt_token");

        AuthResponse response = authService.loginCustomer(req);

        assertNotNull(response);
        assertEquals("CUSTOMER", response.getRole());
        assertEquals(1L, response.getUserId());
        assertEquals("Alice", response.getName());
    }

    @Test
    void loginCustomer_UnknownEmail_ShouldThrowUnauthorizedException() {
        LoginRequest req = LoginRequest.builder()
                .email("unknown@example.com").password("Password123").build();

        when(customerRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.loginCustomer(req));
    }

    @Test
    void loginCustomer_WrongPassword_ShouldThrowUnauthorizedException() {
        LoginRequest req = LoginRequest.builder()
                .email("alice@example.com").password("WrongPass").build();

        when(customerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("WrongPass", "hashed_password")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.loginCustomer(req));

        assertEquals("Invalid credentials", ex.getMessage());
    }

    // ── loginOwner ────────────────────────────────────────────────────────────

    @Test
    void loginOwner_ValidCredentials_ShouldReturnAuthResponseWithRoleSalonOwner() {
        LoginRequest req = LoginRequest.builder()
                .email("owner@example.com").password("Password123").build();

        when(salonOwnerRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(salonOwner));
        when(passwordEncoder.matches("Password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(eq("owner@example.com"), eq("SALON_OWNER"), eq(10L), eq("Hyderabad")))
                .thenReturn("jwt_token");

        AuthResponse response = authService.loginOwner(req);

        assertNotNull(response);
        assertEquals("SALON_OWNER", response.getRole());
        assertEquals(10L, response.getUserId());
        assertEquals("Owner", response.getName());
    }

    @Test
    void loginOwner_UnknownEmail_ShouldThrowUnauthorizedException() {
        LoginRequest req = LoginRequest.builder()
                .email("nobody@example.com").password("Password123").build();

        when(salonOwnerRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.loginOwner(req));
    }

    @Test
    void loginOwner_WrongPassword_ShouldThrowUnauthorizedException() {
        LoginRequest req = LoginRequest.builder()
                .email("owner@example.com").password("WrongPass").build();

        when(salonOwnerRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(salonOwner));
        when(passwordEncoder.matches("WrongPass", "hashed_password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.loginOwner(req));
    }

    // ── registerProfessional ──────────────────────────────────────────────────

    @Test
    void registerProfessional_ValidRequest_ShouldReturnAuthResponseWithRoleProfessional() {
        ProfessionalRegisterRequest req = ProfessionalRegisterRequest.builder()
                .name("Bob").email("bob@example.com")
                .password("Password123").city("Hyderabad")
                .specialization("Haircut").build();

        when(professionalRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(salonOwnerRepository.findByCity("Hyderabad")).thenReturn(Optional.of(salonOwner));
        when(passwordEncoder.encode("Password123")).thenReturn("hashed_password");
        when(professionalRepository.save(any(Professional.class))).thenReturn(professional);
        when(jwtUtil.generateToken(eq("bob@example.com"), eq("PROFESSIONAL"), eq(2L), eq("Hyderabad")))
                .thenReturn("jwt_token");

        AuthResponse response = authService.registerProfessional(req);

        assertNotNull(response);
        assertEquals("PROFESSIONAL", response.getRole());
        assertEquals(2L, response.getUserId());
        assertEquals("Bob", response.getName());
        verify(professionalRepository).save(any(Professional.class));
    }

    @Test
    void registerProfessional_DuplicateEmail_ShouldThrowValidationException() {
        ProfessionalRegisterRequest req = ProfessionalRegisterRequest.builder()
                .name("Bob").email("bob@example.com")
                .password("Password123").city("Hyderabad")
                .specialization("Haircut").build();

        when(professionalRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(professional));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.registerProfessional(req));

        assertEquals("Email already registered", ex.getMessage());
        verify(professionalRepository, never()).save(any());
    }

    @Test
    void registerProfessional_NoCityOwner_ShouldThrowValidationException() {
        ProfessionalRegisterRequest req = ProfessionalRegisterRequest.builder()
                .name("Bob").email("bob@example.com")
                .password("Password123").city("UnknownCity")
                .specialization("Haircut").build();

        when(professionalRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(salonOwnerRepository.findByCity("UnknownCity")).thenReturn(Optional.empty());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.registerProfessional(req));

        assertEquals("No salon available in selected city", ex.getMessage());
        verify(professionalRepository, never()).save(any());
    }

    // ── loginAdmin ────────────────────────────────────────────────────────────

    @Test
    void loginAdmin_ValidCredentials_ShouldReturnAuthResponseWithRoleAdmin() {
        AdminLoginRequest req = AdminLoginRequest.builder()
                .username("admin").password("password").build();

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(eq("admin"), eq("ADMIN"), eq(99L), isNull()))
                .thenReturn("jwt_token");

        AuthResponse response = authService.loginAdmin(req);

        assertNotNull(response);
        assertEquals("ADMIN", response.getRole());
        assertEquals(99L, response.getUserId());
        assertEquals("admin", response.getName());
    }

    @Test
    void loginAdmin_UnknownUsername_ShouldThrowUnauthorizedException() {
        AdminLoginRequest req = AdminLoginRequest.builder()
                .username("nobody").password("password").build();

        when(adminRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.loginAdmin(req));
    }

    @Test
    void loginAdmin_WrongPassword_ShouldThrowUnauthorizedException() {
        AdminLoginRequest req = AdminLoginRequest.builder()
                .username("admin").password("wrongpass").build();

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrongpass", "hashed_password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.loginAdmin(req));
    }
}
