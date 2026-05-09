package com.salon.controller;

import com.salon.dto.request.AdminLoginRequest;
import com.salon.dto.request.CustomerRegisterRequest;
import com.salon.dto.request.LoginRequest;
import com.salon.dto.request.ProfessionalRegisterRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/customer/register")
    public ResponseEntity<AuthResponse> registerCustomer(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCustomer(request));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<AuthResponse> loginCustomer(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginCustomer(request));
    }

    @PostMapping("/owner/login")
    public ResponseEntity<AuthResponse> loginOwner(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginOwner(request));
    }

    @PostMapping("/professional/register")
    public ResponseEntity<AuthResponse> registerProfessional(@Valid @RequestBody ProfessionalRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerProfessional(request));
    }

    @PostMapping("/professional/login")
    public ResponseEntity<AuthResponse> loginProfessional(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginProfessional(request));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }
}
