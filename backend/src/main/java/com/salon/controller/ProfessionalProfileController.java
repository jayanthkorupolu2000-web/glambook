package com.salon.controller;

import com.salon.dto.request.ProfessionalProfileUpdateRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.dto.response.ProfessionalProfileResponse;
import com.salon.dto.response.ServiceResponse;
import com.salon.entity.Appointment;
import com.salon.entity.Professional;
import com.salon.entity.Service;
import com.salon.entity.UserStatus;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.FavoriteServiceRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.ServiceRepository;
import com.salon.security.JwtUtil;
import com.salon.service.ProfessionalProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/professionals")
@RequiredArgsConstructor
@Tag(name = "Professional Profile", description = "Professional profile management")
public class ProfessionalProfileController {

    private final ProfessionalProfileService profileService;
    private final ServiceRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;
    private final FavoriteServiceRepository favoriteServiceRepository;
    private final JwtUtil jwtUtil;

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Update professional profile")
    public ResponseEntity<ProfessionalProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfessionalProfileUpdateRequest dto) {
        return ResponseEntity.ok(profileService.updateProfile(id, dto));
    }

    @PostMapping("/{id}/profile/photo")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Upload profile photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String url = profileService.uploadProfilePhoto(id, file);
        return ResponseEntity.ok(Map.of("photoUrl", url));
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get professional profile")
    public ResponseEntity<ProfessionalProfileResponse> getProfile(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // If the caller is the professional themselves, look up by email from JWT
        // This handles the case where the stored userId doesn't match the DB id
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String email = jwtUtil.extractEmail(token);
                String role  = jwtUtil.extractRole(token);

                if ("PROFESSIONAL".equals(role)) {
                    Professional prof = professionalRepository.findByEmail(email).orElse(null);
                    if (prof != null) {
                        return ResponseEntity.ok(profileService.getProfileById(prof.getId()));
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback: look up by path variable id
        return ResponseEntity.ok(profileService.getProfileById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search professionals by city, price range, rating and other filters")
    public ResponseEntity<List<ProfessionalProfileResponse>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String targetGroup,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean homeAvailable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating) {

        // Validate price range
        if (minPrice != null && minPrice < 0)
            throw new com.salon.exception.ValidationException(
                    "Please provide a valid minPrice — must be greater than or equal to 0");
        if (maxPrice != null && maxPrice < 0)
            throw new com.salon.exception.ValidationException(
                    "Please provide a valid maxPrice — must be greater than or equal to 0");
        if (minPrice != null && maxPrice != null && maxPrice < minPrice)
            throw new com.salon.exception.ValidationException(
                    "maxPrice must be greater than or equal to minPrice");

        return ResponseEntity.ok(profileService.searchProfessionalsWithPrice(
                city, targetGroup, category, homeAvailable, keyword, minPrice, maxPrice, minRating));
    }

    @GetMapping("/{id}/services")
    @Operation(summary = "Get services offered by a professional")
    public ResponseEntity<List<ServiceResponse>> getServices(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Service> services = serviceRepository.findByProfessionalId(id);
        if (activeOnly) services = services.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .collect(Collectors.toList());
        List<ServiceResponse> result = services.stream().map(s -> ServiceResponse.builder()
                .id(s.getId()).name(s.getName()).category(s.getCategory())
                .gender(s.getGender() != null ? s.getGender().name() : null)
                .price(s.getPrice()).durationMins(s.getDurationMins())
                .discountPct(s.getDiscountPct() != null ? s.getDiscountPct() : BigDecimal.ZERO)
                .isActive(Boolean.TRUE.equals(s.getIsActive()))
                .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}/services/{serviceId}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Transactional
    @Operation(summary = "Delete a service owned by a professional")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id,
            @PathVariable Long serviceId) {
        Service s = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new com.salon.exception.ResourceNotFoundException("Service not found"));
        // Ensure the service belongs to this professional
        if (s.getProfessionalId() == null || !s.getProfessionalId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // Remove any customer favorites referencing this service first,
        // otherwise the FK on favorite_services will block the delete.
        favoriteServiceRepository.deleteByServiceId(serviceId);
        // Null out service_id on appointments to preserve booking history
        // while removing the FK dependency on the appointments table.
        appointmentRepository.detachService(serviceId);
        serviceRepository.deleteById(serviceId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/services/{serviceId}/toggle")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Toggle service active/inactive")
    public ResponseEntity<ServiceResponse> toggleService(
            @PathVariable Long id,
            @PathVariable Long serviceId) {
        Service s = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new com.salon.exception.ResourceNotFoundException("Service not found"));
        s.setIsActive(!Boolean.TRUE.equals(s.getIsActive()));
        Service saved = serviceRepository.save(s);
        return ResponseEntity.ok(ServiceResponse.builder()
                .id(saved.getId()).name(saved.getName()).category(saved.getCategory())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .price(saved.getPrice()).durationMins(saved.getDurationMins())
                .discountPct(saved.getDiscountPct() != null ? saved.getDiscountPct() : BigDecimal.ZERO)
                .isActive(Boolean.TRUE.equals(saved.getIsActive()))
                .build());
    }

    @PostMapping("/{id}/services")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Add a service for a professional")
    public ResponseEntity<ServiceResponse> addService(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        // Find the professional to get their salon owner
        com.salon.entity.Professional prof = professionalRepository.findById(id).orElse(null);

        Service s = new Service();
        s.setProfessionalId(id);
        s.setName((String) body.get("name"));
        s.setCategory((String) body.get("category"));
        if (body.get("price") != null) s.setPrice(new BigDecimal(body.get("price").toString()));
        if (body.get("durationMins") != null) s.setDurationMins(Integer.parseInt(body.get("durationMins").toString()));
        if (body.get("discountPct") != null) {
            s.setDiscountPct(new BigDecimal(body.get("discountPct").toString()));
        } else {
            s.setDiscountPct(BigDecimal.ZERO);
        }
        s.setIsActive(true);

        // Map targetGroup → Gender enum
        String tg = body.get("targetGroup") != null ? body.get("targetGroup").toString() : "WOMEN";
        try { s.setGender(com.salon.entity.Gender.valueOf(tg)); }
        catch (Exception e) { s.setGender(com.salon.entity.Gender.WOMEN); }

        Service saved = serviceRepository.save(s);
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceResponse.builder()
                .id(saved.getId()).name(saved.getName()).category(saved.getCategory())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .price(saved.getPrice()).durationMins(saved.getDurationMins())
                .discountPct(saved.getDiscountPct() != null ? saved.getDiscountPct() : BigDecimal.ZERO)
                .isActive(Boolean.TRUE.equals(saved.getIsActive()))
                .build());
    }

    @GetMapping("/{id}/appointments")
    @PreAuthorize("hasAnyRole('PROFESSIONAL','SALON_OWNER','ADMIN')")
    @Operation(summary = "Get all appointments for a professional")
    public ResponseEntity<List<AppointmentResponse>> getProfessionalAppointments(@PathVariable Long id) {
        List<Appointment> appts = appointmentRepository.findByProfessionalIdOrderByDateTimeDesc(id);
        List<AppointmentResponse> result = appts.stream().map(a -> {
            AppointmentResponse r = new AppointmentResponse();
            r.setId(a.getId());
            r.setStatus(a.getStatus().name());
            r.setScheduledAt(a.getDateTime());
            if (a.getCustomer() != null) {
                r.setCustomerId(a.getCustomer().getId());
                r.setCustomerName(a.getCustomer().getName());
            }
            if (a.getProfessional() != null) {
                r.setProfessionalId(a.getProfessional().getId());
                r.setProfessionalName(a.getProfessional().getName());
            }
            if (a.getService() != null) {
                r.setServiceId(a.getService().getId());
                r.setServiceName(a.getService().getName());
            }
            return r;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
