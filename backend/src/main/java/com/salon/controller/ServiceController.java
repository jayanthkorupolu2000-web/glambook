package com.salon.controller;

import com.salon.dto.response.ServiceResponse;
import com.salon.entity.Gender;
import com.salon.entity.Service;
import com.salon.repository.ServiceRepository;
import com.salon.service.FavoritesService;
import com.salon.service.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Service management endpoints")
public class ServiceController {

    private final ServiceService serviceService;
    private final ServiceRepository serviceRepository;
    private final FavoritesService favoritesService;

    @GetMapping
    @Operation(summary = "Get all services grouped by gender and category")
    public ResponseEntity<Map<String, Map<String, List<ServiceResponse>>>> getAllServices() {
        return ResponseEntity.ok(serviceService.getAllServicesGrouped());
    }

    @GetMapping("/list")
    @Operation(summary = "Get all services as a flat list")
    public ResponseEntity<List<ServiceResponse>> getAllServicesList(
            @RequestParam(required = false) Long customerId) {
        List<ServiceResponse> services = serviceService.getAllServicesList();
        return ResponseEntity.ok(favoritesService.enrichServices(services, customerId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Add a new service (Salon Owner only)")
    public ResponseEntity<ServiceResponse> addService(@RequestBody Map<String, Object> body) {
        Service s = new Service();
        s.setName((String) body.get("name"));
        s.setCategory((String) body.get("category"));
        if (body.get("price") != null)
            s.setPrice(new BigDecimal(body.get("price").toString()));
        if (body.get("durationMins") != null)
            s.setDurationMins(Integer.parseInt(body.get("durationMins").toString()));
        try {
            String gender = body.get("gender") != null ? body.get("gender").toString() : "WOMEN";
            s.setGender(Gender.valueOf(gender));
        } catch (Exception e) {
            s.setGender(Gender.WOMEN);
        }
        s.setIsActive(true);
        Service saved = serviceRepository.save(s);
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceResponse.builder()
                .id(saved.getId()).name(saved.getName()).category(saved.getCategory())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .price(saved.getPrice()).durationMins(saved.getDurationMins()).build());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Toggle service active/inactive (Salon Owner)")
    public ResponseEntity<ServiceResponse> toggleService(@PathVariable Long id) {
        Service s = serviceRepository.findById(id)
                .orElseThrow(() -> new com.salon.exception.ResourceNotFoundException("Service not found"));
        s.setIsActive(!Boolean.TRUE.equals(s.getIsActive()));
        Service saved = serviceRepository.save(s);
        return ResponseEntity.ok(ServiceResponse.builder()
                .id(saved.getId()).name(saved.getName()).category(saved.getCategory())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .price(saved.getPrice()).durationMins(saved.getDurationMins())
                .isActive(Boolean.TRUE.equals(saved.getIsActive())).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Delete a service (Salon Owner only)")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
