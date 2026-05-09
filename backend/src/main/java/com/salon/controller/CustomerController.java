package com.salon.controller;

import com.salon.dto.response.CustomerResponse;
import com.salon.entity.Customer;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return ResponseEntity.ok(toResponse(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (body.containsKey("name") && body.get("name") != null)
            c.setName(body.get("name"));
        if (body.containsKey("phone"))
            c.setPhone(body.get("phone"));
        if (body.containsKey("city") && body.get("city") != null)
            c.setCity(body.get("city"));
        if (body.containsKey("emergencyContact"))
            c.setEmergencyContact(body.get("emergencyContact"));
        if (body.containsKey("medicalNotes"))
            c.setMedicalNotes(body.get("medicalNotes"));

        customerRepository.save(c);
        return ResponseEntity.ok(toResponse(c));
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .city(c.getCity())
                .emergencyContact(c.getEmergencyContact())
                .medicalNotes(c.getMedicalNotes())
                .build();
    }
}
