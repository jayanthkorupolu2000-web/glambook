package com.salon.service;

import com.salon.dto.request.UpdateProfileRequest;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.dto.response.SalonOwnerResponse;
import com.salon.dto.response.ServiceResponse;
import com.salon.entity.Professional;
import com.salon.entity.Review;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.UnauthorizedException;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.ReviewRepository;
import com.salon.repository.ServiceRepository;
import com.salon.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final ReviewRepository reviewRepository;
    private final ServiceRepository serviceRepository;
    private final JwtUtil jwtUtil;

    public Page<ProfessionalResponse> getProfessionalsByCity(String city, Pageable pageable) {
        return professionalRepository.findByCity(city, pageable).map(this::mapToResponse);
    }

    public List<ProfessionalResponse> getAllProfessionals() {
        return professionalRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProfessionalResponse getProfessionalById(Long id) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found with id: " + id));
        return mapToResponse(professional);
    }

    public ProfessionalResponse updateProfile(Long id, UpdateProfileRequest request, HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid authorization header");
        }
        String token = authHeader.substring(7);
        Long authenticatedUserId = jwtUtil.extractUserId(token);

        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found with id: " + id));

        if (!professional.getId().equals(authenticatedUserId)) {
            throw new UnauthorizedException("You can only update your own profile");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty())
            professional.setName(request.getName().trim());
        if (request.getSpecialization() != null && !request.getSpecialization().trim().isEmpty())
            professional.setSpecialization(request.getSpecialization().trim());
        if (request.getExperienceYears() != null && request.getExperienceYears() >= 0)
            professional.setExperienceYears(request.getExperienceYears());

        return mapToResponse(professionalRepository.save(professional));
    }

    private ProfessionalResponse mapToResponse(Professional professional) {
        List<Review> reviews = reviewRepository.findByProfessionalId(professional.getId());
        Double rating = calculateAverageRating(reviews);

        // 1st priority: services explicitly linked to this professional (professional_id FK)
        List<com.salon.entity.Service> ownServices =
                serviceRepository.findByProfessionalId(professional.getId());

        if (ownServices.isEmpty() && professional.getSpecialization() != null) {
            String spec = professional.getSpecialization().toLowerCase().trim();

            // 2nd: exact name match
            ownServices = serviceRepository.findByNameContainingIgnoreCase(spec);

            // 3rd: exact category match
            if (ownServices.isEmpty()) {
                ownServices = serviceRepository.findByCategoryIgnoreCase(spec);
            }

            // 4th: map known specialization keywords to categories
            if (ownServices.isEmpty()) {
                String mappedCategory = mapSpecializationToCategory(spec);
                if (mappedCategory != null) {
                    ownServices = serviceRepository.findByCategoryIgnoreCase(mappedCategory);
                }
            }

            // 5th: partial word match on name or category
            if (ownServices.isEmpty()) {
                String[] words = spec.split("\\s+");
                ownServices = serviceRepository.findAll().stream()
                        .filter(s -> {
                            String sName = s.getName() != null ? s.getName().toLowerCase() : "";
                            String sCat  = s.getCategory() != null ? s.getCategory().toLowerCase() : "";
                            for (String w : words) {
                                if (w.length() > 3 && (sName.contains(w) || sCat.contains(w))) return true;
                            }
                            return false;
                        })
                        .limit(3)
                        .collect(Collectors.toList());
            }
        }

        List<ServiceResponse> services = ownServices.stream()
                .map(this::mapToServiceResponse)
                .collect(Collectors.toList());

        return ProfessionalResponse.builder()
                .id(professional.getId())
                .name(professional.getName())
                .email(professional.getEmail())
                .city(professional.getCity())
                .specialization(professional.getSpecialization())
                .experienceYears(professional.getExperienceYears())
                .salonOwner(mapToSalonOwnerResponse(professional))
                .rating(rating)
                .services(services)
                .build();
    }

    private SalonOwnerResponse mapToSalonOwnerResponse(Professional professional) {
        if (professional.getSalonOwner() == null) return null;
        return SalonOwnerResponse.builder()
                .id(professional.getSalonOwner().getId())
                .name(professional.getSalonOwner().getName())
                .salonName(professional.getSalonOwner().getSalonName())
                .city(professional.getSalonOwner().getCity())
                .email(professional.getSalonOwner().getEmail())
                .phone(professional.getSalonOwner().getPhone())
                .build();
    }

    private ServiceResponse mapToServiceResponse(com.salon.entity.Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory())
                .gender(service.getGender() != null ? service.getGender().name() : null)
                .price(service.getPrice())
                .durationMins(service.getDurationMins())
                .build();
    }

    private Double calculateAverageRating(List<Review> reviews) {
        if (reviews.isEmpty()) return null;
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    /**
     * Maps common specialization strings to service categories in the DB.
     * Extend this map as new specializations are added.
     */
    private String mapSpecializationToCategory(String spec) {
        if (spec == null) return null;
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        // Hair-related
        map.put("hair styling", "Hair");
        map.put("hair stylist", "Hair");
        map.put("hair", "Hair");
        map.put("hairstyling", "Hair");
        map.put("hairstylist", "Hair");
        // Nail-related
        map.put("nail art", "Nails");
        map.put("nail artist", "Nails");
        map.put("nails", "Nails");
        map.put("nail", "Nails");
        map.put("manicure", "Nails");
        map.put("pedicure", "Nails");
        // Skin-related
        map.put("skin care", "Skin");
        map.put("skincare", "Skin");
        map.put("facial", "Skin");
        map.put("skin", "Skin");
        // Makeup-related
        map.put("makeup", "Makeup");
        map.put("make up", "Makeup");
        map.put("bridal makeup", "Makeup");
        map.put("makeup artist", "Makeup");
        // Beard-related
        map.put("beard", "Beard");
        map.put("beard styling", "Beard");
        map.put("beard trim", "Beard");
        // Body-related
        map.put("body massage", "Body");
        map.put("massage", "Body");
        map.put("body", "Body");
        // Kids
        map.put("kids", "Hair");
        map.put("grooming", "Grooming");

        return map.get(spec.toLowerCase().trim());
    }
}
