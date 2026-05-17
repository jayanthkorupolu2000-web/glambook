package com.salon.auth;

import com.salon.dto.request.AdminLoginRequest;
import com.salon.dto.request.CustomerRegisterRequest;
import com.salon.dto.request.LoginRequest;
import com.salon.dto.request.ProfessionalRegisterRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.entity.*;
import com.salon.exception.ConflictException;
import com.salon.exception.UnauthorizedException;
import com.salon.exception.ValidationException;
import com.salon.repository.*;
import com.salon.security.JwtUtil;
import com.salon.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private SalonOwnerRepository salonOwnerRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthService authService;

    private Customer customer;
    private Professional professional;
    private SalonOwner salonOwner;
    private Admin admin;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L).name("Alice").email("alice@gmail.com")
                .password("encoded_pass").city("Hyderabad").build();

        salonOwner = SalonOwner.builder()
                .id(2L).name("Owner").salonName("Glamour Salon")
                .email("owner@gmail.com").password("encoded_pass").city("Hyderabad").build();

        professional = Professional.builder()
                .id(3L).name("Bob").email("bob@gmail.com")
                .password("encoded_pass").city("Hyderabad")
                .specialization("Hair Styling").salonOwner(salonOwner).build();

        admin = Admin.builder()
                .id(4L).username("admin").password("encoded_pass").build();
    }

    // ── registerCustomer ──────────────────────────────────────────────────────

    @Test
    void registerCustomer_NewEmail_ShouldReturnToken() {
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setName("Alice"); req.setEmail("alice@gmail.com");
        req.setPassword("Pass@123"); req.setCity("Hyderabad");

        when(customerRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Pass@123")).thenReturn("encoded_pass");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(jwtUtil.generateToken(anyString(), eq("CUSTOMER"), anyLong(), anyString()))
                .thenReturn("jwt_token");

        AuthResponse response = authService.registerCustomer(req);

        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals(1L, response.getUserId());
        assertEquals("Alice", response.getName());
    }

    @Test
    void registerCustomer_DuplicateEmail_ShouldThrowValidationException() {
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setEmail("alice@gmail.com");

        when(customerRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.of(customer));

        assertThrows(ValidationException.class, () -> authService.registerCustomer(req));
        verify(customerRepository, never()).save(any());
    }

    // ── loginCustomer ─────────────────────────────────────────────────────────

    @Test
    void loginCustomer_ValidCredentials_ShouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@gmail.com"); req.setPassword("Pass@123");

        when(customerRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Pass@123", "encoded_pass")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), eq("CUSTOMER"), anyLong(), anyString()))
                .thenReturn("jwt_token");

        AuthResponse response = authService.loginCustomer(req);

        assertNotNull(response);
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("jwt_token", response.getToken());
    }

    @Test
    void loginCustomer_WrongPassword_ShouldThrowUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@gmail.com"); req.setPassword("wrong");

        when(customerRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "encoded_pass")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.loginCustomer(req));
    }

    @Test
    void loginCustomer_EmailNotFound_ShouldThrowUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@gmail.com"); req.setPassword("pass");

        when(customerRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.loginCustomer(req));
    }

    @Test
    void loginCustomer_SuspendedAccount_ShouldThrowUnauthorizedException() {
        customer.setStatus(UserStatus.SUSPENDED);
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@gmail.com"); req.setPassword("Pass@123");

        when(customerRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Pass@123", "encoded_pass")).thenReturn(true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.loginCustomer(req));
        assertTrue(ex.getMessage().contains("suspended"));
    }

    // ── loginOwner ────────────────────────────────────────────────────────────

    @Test
    void loginOwner_ValidCredentials_ShouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("owner@gmail.com"); req.setPassword("Pass@123");

        when(salonOwnerRepository.findByEmail("owner@gmail.com")).thenReturn(Optional.of(salonOwner));
        when(passwordEncoder.matches("Pass@123", "encoded_pass")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), eq("SALON_OWNER"), anyLong(), anyString()))
                .thenReturn("owner_token");

        AuthResponse response = authService.loginOwner(req);

        assertEquals("SALON_OWNER", response.getRole());
        assertEquals("owner_token", response.getToken());
    }

    @Test
    void loginOwner_WrongPassword_ShouldThrowUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("owner@gmail.com"); req.setPassword("wrong");

        when(salonOwnerRepository.findByEmail("owner@gmail.com")).thenReturn(Optional.of(salonOwner));
        when(passwordEncoder.matches("wrong", "encoded_pass")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.loginOwner(req));
    }

    // ── loginProfessional ─────────────────────────────────────────────────────

    @Test
    void loginProfessional_ValidCredentials_ShouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("bob@gmail.com"); req.setPassword("Pass@123");

        when(professionalRepository.findByEmail("bob@gmail.com")).thenReturn(Optional.of(professional));
        when(passwordEncoder.matches("Pass@123", "encoded_pass")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), eq("PROFESSIONAL"), anyLong(), anyString()))
                .thenReturn("prof_token");

        AuthResponse response = authService.loginProfessional(req);

        assertEquals("PROFESSIONAL", response.getRole());
        assertEquals("prof_token", response.getToken());
    }

    @Test
    void loginProfessional_NotFound_ShouldThrowUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@gmail.com"); req.setPassword("pass");

        when(professionalRepository.findByEmail("nobody@gmail.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.loginProfessional(req));
    }

    // ── registerProfessional ──────────────────────────────────────────────────

    @Test
    void registerProfessional_ValidRequest_ShouldReturnToken() {
        ProfessionalRegisterRequest req = new ProfessionalRegisterRequest();
        req.setName("Bob"); req.setEmail("bob@gmail.com");
        req.setPassword("Pass@123"); req.setCity("Hyderabad");
        req.setSpecialization("Hair Styling");

        when(professionalRepository.findByEmail("bob@gmail.com")).thenReturn(Optional.empty());
        when(salonOwnerRepository.findByCity("Hyderabad")).thenReturn(Optional.of(salonOwner));
        when(passwordEncoder.encode("Pass@123")).thenReturn("encoded_pass");
        when(professionalRepository.save(any(Professional.class))).thenReturn(professional);
        when(jwtUtil.generateToken(anyString(), eq("PROFESSIONAL"), anyLong(), anyString()))
                .thenReturn("prof_token");

        AuthResponse response = authService.registerProfessional(req);

        assertNotNull(response);
        assertEquals("PROFESSIONAL", response.getRole());
        assertEquals("prof_token", response.getToken());
    }

    @Test
    void registerProfessional_DuplicateEmail_ShouldThrowValidationException() {
        ProfessionalRegisterRequest req = new ProfessionalRegisterRequest();
        req.setEmail("bob@gmail.com");

        when(professionalRepository.findByEmail("bob@gmail.com")).thenReturn(Optional.of(professional));

        assertThrows(ValidationException.class, () -> authService.registerProfessional(req));
        verify(professionalRepository, never()).save(any());
    }

    @Test
    void registerProfessional_NoCityOwner_ShouldThrowValidationException() {
        ProfessionalRegisterRequest req = new ProfessionalRegisterRequest();
        req.setEmail("new@gmail.com"); req.setCity("UnknownCity");

        when(professionalRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(salonOwnerRepository.findByCity("UnknownCity")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> authService.registerProfessional(req));
    }

    // ── loginAdmin ────────────────────────────────────────────────────────────

    @Test
    void loginAdmin_ValidCredentials_ShouldReturnToken() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin"); req.setPassword("admin_pass");

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin_pass", "encoded_pass")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), eq("ADMIN"), anyLong(), isNull()))
                .thenReturn("admin_token");

        AuthResponse response = authService.loginAdmin(req);

        assertEquals("ADMIN", response.getRole());
        assertEquals("admin_token", response.getToken());
    }

    @Test
    void loginAdmin_WrongPassword_ShouldThrowUnauthorizedException() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin"); req.setPassword("wrong");

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "encoded_pass")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.loginAdmin(req));
    }

    @Test
    void loginAdmin_NotFound_ShouldThrowUnauthorizedException() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("ghost"); req.setPassword("pass");

        when(adminRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.loginAdmin(req));
    }
}
