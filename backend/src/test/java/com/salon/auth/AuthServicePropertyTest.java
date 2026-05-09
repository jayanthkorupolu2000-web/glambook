package com.salon.auth;

import com.salon.dto.request.CustomerRegisterRequest;
import com.salon.dto.request.LoginRequest;
import com.salon.dto.request.ProfessionalRegisterRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.exception.UnauthorizedException;
import com.salon.exception.ValidationException;
import com.salon.repository.CustomerRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.AuthService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for AuthService.
 *
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 3.1, 3.2
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class AuthServicePropertyTest {

    private static final List<String> VALID_CITIES = List.of(
            "Visakhapatnam", "Vijayawada", "Hyderabad", "Ananthapur", "Khammam"
    );

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private SalonOwnerRepository salonOwnerRepository;

    /**
     * Ensure one SalonOwner per city is present before each test run.
     */
    @BeforeEach
    void seedSalonOwners() {
        String hashedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String[][] owners = {
            {"Ravi Kumar",   "Ravi Salon",   "Visakhapatnam", "ravi@salon.com",   "9000000001"},
            {"Priya Reddy",  "Priya Salon",  "Vijayawada",    "priya@salon.com",  "9000000002"},
            {"Suresh Rao",   "Suresh Salon", "Hyderabad",     "suresh@salon.com", "9000000003"},
            {"Anita Sharma", "Anita Salon",  "Ananthapur",    "anita@salon.com",  "9000000004"},
            {"Kiran Babu",   "Kiran Salon",  "Khammam",       "kiran@salon.com",  "9000000005"}
        };
        for (String[] o : owners) {
            if (salonOwnerRepository.findByEmail(o[3]).isEmpty()) {
                salonOwnerRepository.save(SalonOwner.builder()
                        .name(o[0]).salonName(o[1]).city(o[2])
                        .email(o[3]).password(hashedPassword).phone(o[4])
                        .build());
            }
        }
    }

    /**
     * Property 1: Customer Registration Round-Trip
     *
     * For any valid customer registration payload, registering and then logging in
     * with the same credentials should return a JWT containing role CUSTOMER and the same userId.
     *
     * Validates: Requirements 1.1, 1.2
     */
    @Property(tries = 10)
    @Transactional
    void customerRegisterThenLoginReturnsSameUserIdAndRoleCustomer(
            @ForAll @StringLength(min = 2, max = 20) @AlphaChars String name,
            @ForAll @StringLength(min = 5, max = 15) @AlphaChars String emailPrefix,
            @ForAll @StringLength(min = 8, max = 20) @AlphaChars String passwordSuffix) {

        String uniqueEmail = emailPrefix + System.nanoTime() + "@test.com";
        String password = "Pass1234" + passwordSuffix;
        String city = VALID_CITIES.get(Math.abs(emailPrefix.hashCode()) % VALID_CITIES.size());

        CustomerRegisterRequest registerRequest = CustomerRegisterRequest.builder()
                .name(name)
                .email(uniqueEmail)
                .password(password)
                .city(city)
                .build();

        AuthResponse registerResponse = authService.registerCustomer(registerRequest);

        assertThat(registerResponse.getRole()).isEqualTo("CUSTOMER");
        assertThat(registerResponse.getUserId()).isNotNull();

        LoginRequest loginRequest = LoginRequest.builder()
                .email(uniqueEmail)
                .password(password)
                .build();

        AuthResponse loginResponse = authService.loginCustomer(loginRequest);

        assertThat(loginResponse.getRole()).isEqualTo("CUSTOMER");
        assertThat(loginResponse.getUserId()).isEqualTo(registerResponse.getUserId());
    }

    /**
     * Property 2: Invalid Credentials Rejected
     *
     * For any email/password pair that was never registered, a login attempt
     * should throw UnauthorizedException (maps to 401).
     *
     * Validates: Requirements 1.3, 2.2, 4.2
     */
    @Property(tries = 10)
    void unregisteredEmailReturns401OnLogin(
            @ForAll @StringLength(min = 5, max = 15) @AlphaChars String emailPrefix,
            @ForAll @StringLength(min = 8, max = 20) @AlphaChars String password) {

        String unregisteredEmail = "unregistered_" + emailPrefix + System.nanoTime() + "@nowhere.com";

        LoginRequest loginRequest = LoginRequest.builder()
                .email(unregisteredEmail)
                .password(password)
                .build();

        assertThatThrownBy(() -> authService.loginCustomer(loginRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * Property 3: Duplicate Email Registration Rejected
     *
     * For any email that already exists in the system, a second registration attempt
     * with that email should throw ValidationException (maps to 400).
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 10)
    @Transactional
    void duplicateEmailRegistrationThrowsValidationException(
            @ForAll @StringLength(min = 2, max = 20) @AlphaChars String name,
            @ForAll @StringLength(min = 5, max = 15) @AlphaChars String emailPrefix) {

        String uniqueEmail = emailPrefix + System.nanoTime() + "@dup.com";
        String city = VALID_CITIES.get(Math.abs(emailPrefix.hashCode()) % VALID_CITIES.size());

        CustomerRegisterRequest first = CustomerRegisterRequest.builder()
                .name(name)
                .email(uniqueEmail)
                .password("Password123")
                .city(city)
                .build();

        authService.registerCustomer(first);

        CustomerRegisterRequest second = CustomerRegisterRequest.builder()
                .name(name)
                .email(uniqueEmail)
                .password("DifferentPass456")
                .city(city)
                .build();

        assertThatThrownBy(() -> authService.registerCustomer(second))
                .isInstanceOf(ValidationException.class);
    }

    /**
     * Property 4: Professional City Assignment Invariant
     *
     * For any professional registration with a valid city, the created Professional record
     * should be assigned to the Salon Owner whose city matches the registration city.
     *
     * Validates: Requirements 3.1, 3.2
     */
    @Property(tries = 5)
    @Transactional
    void professionalRegisteredWithCityXIsAssignedToSalonOwnerOfCityX(
            @ForAll @StringLength(min = 2, max = 20) @AlphaChars String name,
            @ForAll @StringLength(min = 5, max = 15) @AlphaChars String emailPrefix,
            @ForAll("validCities") String city) {

        String uniqueEmail = emailPrefix + System.nanoTime() + "@pro.com";

        ProfessionalRegisterRequest request = ProfessionalRegisterRequest.builder()
                .name(name)
                .email(uniqueEmail)
                .password("Password123")
                .city(city)
                .specialization("Haircut")
                .build();

        AuthResponse response = authService.registerProfessional(request);

        assertThat(response.getRole()).isEqualTo("PROFESSIONAL");
        assertThat(response.getUserId()).isNotNull();

        Optional<Professional> savedProfessional = professionalRepository.findById(response.getUserId());
        assertThat(savedProfessional).isPresent();
        assertThat(savedProfessional.get().getSalonOwner()).isNotNull();
        assertThat(savedProfessional.get().getSalonOwner().getCity()).isEqualTo(city);
    }

    @Provide
    Arbitrary<String> validCities() {
        return Arbitraries.of(VALID_CITIES);
    }
}
