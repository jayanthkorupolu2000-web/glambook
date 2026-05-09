package com.salon.service;

import com.salon.dto.request.AdminLoginRequest;
import com.salon.dto.request.CustomerRegisterRequest;
import com.salon.dto.request.LoginRequest;
import com.salon.dto.request.ProfessionalRegisterRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.entity.Admin;
import com.salon.entity.Customer;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.exception.ConflictException;
import com.salon.exception.UnauthorizedException;
import com.salon.exception.ValidationException;
import com.salon.repository.AdminRepository;
import com.salon.repository.CustomerRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.repository.ServiceRepository;
import com.salon.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final ProfessionalRepository professionalRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ServiceRepository serviceRepository;

    public AuthResponse registerCustomer(CustomerRegisterRequest request) {
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ValidationException("Email already registered");
        }
        Customer customer = Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .city(request.getCity())
                .build();
        customer = customerRepository.save(customer);
        String token = jwtUtil.generateToken(customer.getEmail(), "CUSTOMER", customer.getId(), customer.getCity());
        return AuthResponse.builder()
                .token(token)
                .role("CUSTOMER")
                .userId(customer.getId())
                .name(customer.getName())
                .build();
    }

    public AuthResponse loginCustomer(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(customer.getEmail(), "CUSTOMER", customer.getId(), customer.getCity());
        return AuthResponse.builder()
                .token(token)
                .role("CUSTOMER")
                .userId(customer.getId())
                .name(customer.getName())
                .build();
    }

    public AuthResponse loginOwner(LoginRequest request) {
        SalonOwner owner = salonOwnerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), owner.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(owner.getEmail(), "SALON_OWNER", owner.getId(), owner.getCity());
        return AuthResponse.builder()
                .token(token)
                .role("SALON_OWNER")
                .userId(owner.getId())
                .name(owner.getName())
                .build();
    }

    public AuthResponse loginProfessional(LoginRequest request) {
        Professional professional = professionalRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), professional.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(professional.getEmail(), "PROFESSIONAL", professional.getId(), professional.getCity());
        return AuthResponse.builder()
                .token(token)
                .role("PROFESSIONAL")
                .userId(professional.getId())
                .name(professional.getName())
                .build();
    }

    public AuthResponse registerProfessional(ProfessionalRegisterRequest request) {
        if (professionalRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ValidationException("Email already registered");
        }
        SalonOwner owner = salonOwnerRepository.findByCity(request.getCity())
                .orElseThrow(() -> new ValidationException("No salon available in selected city"));

        // Resolve specialization from service if serviceId provided
        String specialization = request.getSpecialization();
        com.salon.entity.Service linkedService = null;
        if (request.getServiceId() != null) {
            linkedService = serviceRepository.findById(request.getServiceId()).orElse(null);
            if (linkedService != null) {
                specialization = linkedService.getName();
            }
        }

        Professional professional = Professional.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .city(request.getCity())
                .specialization(specialization)
                .salonOwner(owner)
                .build();
        professional = professionalRepository.save(professional);

        // Link the service to this professional
        if (linkedService != null) {
            linkedService.setProfessionalId(professional.getId());
            linkedService.setIsActive(true);  // ensure active
            serviceRepository.save(linkedService);
        }

        String token = jwtUtil.generateToken(professional.getEmail(), "PROFESSIONAL", professional.getId(), professional.getCity());
        return AuthResponse.builder()
                .token(token)
                .role("PROFESSIONAL")
                .userId(professional.getId())
                .name(professional.getName())
                .build();
    }

    public AuthResponse loginAdmin(AdminLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(admin.getUsername(), "ADMIN", admin.getId(), null);
        return AuthResponse.builder()
                .token(token)
                .role("ADMIN")
                .userId(admin.getId())
                .name(admin.getUsername())
                .build();
    }
}
