package com.salon.service.impl;

import com.salon.dto.request.ProfessionalProfileUpdateRequest;
import com.salon.dto.response.PortfolioResponse;
import com.salon.dto.response.ProfessionalProfileResponse;
import com.salon.entity.Professional;
import com.salon.entity.UserStatus;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.PortfolioRepository;
import com.salon.repository.ProfessionalAvailabilityRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.ReviewRepository;
import com.salon.repository.ServiceRepository;
import com.salon.service.FileStorageService;
import com.salon.service.ProfessionalProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalProfileServiceImpl implements ProfessionalProfileService {

    private final ProfessionalRepository professionalRepository;
    private final ReviewRepository reviewRepository;
    private final PortfolioRepository portfolioRepository;
    private final FileStorageService fileStorageService;
    private final ServiceRepository serviceRepository;
    private final ProfessionalAvailabilityRepository availabilityRepository;

    @Override
    @Transactional
    public ProfessionalProfileResponse updateProfile(Long professionalId, ProfessionalProfileUpdateRequest dto) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));

        if (professional.getStatus() == UserStatus.SUSPENDED)
            throw new InvalidOperationException("Suspended professionals cannot update their profile");

        professional.setSpecialization(dto.getSpecialization());
        professional.setExperienceYears(dto.getExperienceYears());
        if (dto.getCertifications() != null)   professional.setCertifications(dto.getCertifications());
        if (dto.getTrainingDetails() != null)  professional.setTrainingDetails(dto.getTrainingDetails());
        if (dto.getServiceAreas() != null)     professional.setServiceAreas(dto.getServiceAreas());
        if (dto.getTravelRadiusKm() != null)   professional.setTravelRadiusKm(dto.getTravelRadiusKm());
        if (dto.getBio() != null)              professional.setBio(dto.getBio());
        if (dto.getInstagramHandle() != null)  professional.setInstagramHandle(dto.getInstagramHandle());
        if (dto.getIsAvailableHome() != null)  professional.setAvailableHome(dto.getIsAvailableHome());
        if (dto.getIsAvailableSalon() != null) professional.setAvailableSalon(dto.getIsAvailableSalon());
        if (dto.getResponseTimeHrs() != null)  professional.setResponseTimeHrs(dto.getResponseTimeHrs());

        return toResponse(professionalRepository.save(professional));
    }

    @Override
    public ProfessionalProfileResponse getProfileById(Long professionalId) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));
        return toResponse(professional);
    }

    @Override
    @Transactional
    public String uploadProfilePhoto(Long professionalId, MultipartFile file) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));
        String url = fileStorageService.storePhoto(file, "profiles");
        professional.setProfilePhotoUrl(url);
        professionalRepository.save(professional);
        return url;
    }

    @Override
    public List<ProfessionalProfileResponse> searchProfessionals(
            String city, String targetGroup, String category, Boolean homeAvailable) {
        return searchProfessionalsWithPrice(city, targetGroup, category,
                homeAvailable, null, null, null, null, null, null);
    }

    @Override
    public List<ProfessionalProfileResponse> searchProfessionalsWithPrice(
            String city, String targetGroup, String category,
            Boolean homeAvailable, Boolean salonAvailable, String keyword,
            Double minPrice, Double maxPrice, Double minRating,
            LocalDate availableDate) {

        return professionalRepository.findAll().stream()
                .filter(p -> p.getStatus() == UserStatus.ACTIVE || p.getStatus() == null)
                .filter(p -> city == null || city.isBlank() || p.getCity().equalsIgnoreCase(city))
                .filter(p -> homeAvailable == null || !homeAvailable || p.isAvailableHome())
                .filter(p -> salonAvailable == null || !salonAvailable || p.isAvailableSalon())
                .filter(p -> keyword == null || keyword.isBlank()
                        || (p.getSpecialization() != null
                            && p.getSpecialization().toLowerCase().contains(keyword.toLowerCase()))
                        || p.getName().toLowerCase().contains(keyword.toLowerCase()))
                // Date filter: professional must have at least one availability slot on that date
                .filter(p -> {
                    if (availableDate == null) return true;
                    return !availabilityRepository
                            .findByProfessionalIdAndAvailDate(p.getId(), availableDate)
                            .isEmpty();
                })
                .map(this::toResponse)
                // targetGroup filter
                .filter(res -> {
                    if (targetGroup == null || targetGroup.isBlank()) return true;
                    if (res.getActiveServices() != null && !res.getActiveServices().isEmpty()) {
                        boolean match = res.getActiveServices().stream().anyMatch(svc ->
                                svc.getGender() != null && svc.getGender().equalsIgnoreCase(targetGroup));
                        if (match) return true;
                        return matchesGroupBySpecialization(res.getSpecialization(), targetGroup);
                    }
                    return matchesGroupBySpecialization(res.getSpecialization(), targetGroup);
                })
                // category filter
                .filter(res -> {
                    if (category == null || category.isBlank()) return true;
                    if (res.getActiveServices() != null && !res.getActiveServices().isEmpty()) {
                        boolean match = res.getActiveServices().stream().anyMatch(svc -> {
                            boolean catMatch = svc.getCategory() != null
                                    && svc.getCategory().equalsIgnoreCase(category);
                            boolean grpMatch = targetGroup == null || targetGroup.isBlank()
                                    || (svc.getGender() != null
                                        && svc.getGender().equalsIgnoreCase(targetGroup));
                            return catMatch && grpMatch;
                        });
                        if (match) return true;
                        return matchesCategoryBySpecialization(res.getSpecialization(), category);
                    }
                    return matchesCategoryBySpecialization(res.getSpecialization(), category);
                })
                // price range filter
                .filter(res -> {
                    if (minPrice == null && (maxPrice == null || maxPrice <= 0)) return true;
                    if (res.getActiveServices() == null || res.getActiveServices().isEmpty()) return false;
                    return res.getActiveServices().stream().anyMatch(svc -> {
                        if (svc.getPrice() == null) return false;
                        double p = svc.getPrice().doubleValue();
                        boolean aboveMin = minPrice == null || p >= minPrice;
                        boolean belowMax = (maxPrice == null || maxPrice <= 0) || p <= maxPrice;
                        return aboveMin && belowMax;
                    });
                })
                // rating filter
                .filter(res -> minRating == null || minRating <= 0
                        || res.getAverageRating() >= minRating)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ProfessionalProfileResponse toResponse(Professional p) {
        ProfessionalProfileResponse res = new ProfessionalProfileResponse();
        res.setId(p.getId());
        res.setProfessionalId(p.getId());
        res.setName(p.getName());
        res.setEmail(p.getEmail());
        res.setCity(p.getCity());
        res.setCityName(p.getCity());
        res.setSpecialization(p.getSpecialization());
        res.setExperienceYears(p.getExperienceYears());
        res.setCertifications(p.getCertifications());
        res.setTrainingDetails(p.getTrainingDetails());
        res.setServiceAreas(p.getServiceAreas());
        res.setTravelRadiusKm(p.getTravelRadiusKm());
        res.setBio(p.getBio());
        res.setInstagramHandle(p.getInstagramHandle());
        res.setAvailableHome(p.isAvailableHome());
        res.setAvailableSalon(p.isAvailableSalon());
        res.setResponseTimeHrs(p.getResponseTimeHrs());
        res.setProfilePhotoUrl(p.getProfilePhotoUrl());
        res.setStatus(p.getStatus() != null ? p.getStatus().name() : "ACTIVE");
        if (p.getSalonOwner() != null) res.setSalonOwnerName(p.getSalonOwner().getName());
        res.setApprovedAt(p.getApprovedAt());

        try {
            Double avg = reviewRepository.findAverageRatingByProfessionalId(p.getId());
            res.setAverageRating(avg != null ? avg : 0.0);
            res.setTotalReviews((int) reviewRepository.countByProfessionalId(p.getId()));
        } catch (Exception e) {
            res.setAverageRating(0.0);
            res.setTotalReviews(0);
        }

        try {
            List<PortfolioResponse> featured = portfolioRepository
                    .findByProfessionalIdAndIsFeatured(p.getId(), true)
                    .stream().limit(3)
                    .map(this::toPortfolioResponse)
                    .collect(Collectors.toList());
            res.setFeaturedPortfolioItems(featured);
        } catch (Exception e) {
            res.setFeaturedPortfolioItems(java.util.Collections.emptyList());
        }

        try {
            List<ProfessionalProfileResponse.ServiceSummary> services = serviceRepository
                    .findByProfessionalId(p.getId())
                    .stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .map(s -> {
                        ProfessionalProfileResponse.ServiceSummary ss =
                                new ProfessionalProfileResponse.ServiceSummary();
                        ss.setId(s.getId());
                        ss.setName(s.getName());
                        ss.setCategory(s.getCategory());
                        ss.setPrice(s.getPrice());
                        ss.setDurationMins(s.getDurationMins());
                        ss.setGender(s.getGender() != null ? s.getGender().name() : null);
                        return ss;
                    })
                    .collect(Collectors.toList());
            res.setActiveServices(services);
        } catch (Exception e) {
            res.setActiveServices(java.util.Collections.emptyList());
        }

        res.setActivePromotions(java.util.Collections.emptyList());
        return res;
    }

    private PortfolioResponse toPortfolioResponse(com.salon.entity.Portfolio port) {
        PortfolioResponse r = new PortfolioResponse();
        r.setId(port.getId());
        r.setProfessionalId(port.getProfessional().getId());
        r.setMediaType(port.getMediaType().name());
        r.setServiceTag(port.getServiceTag());
        r.setCaption(port.getCaption());
        r.setBeforePhotoUrl(port.getBeforePhotoUrl());
        r.setAfterPhotoUrl(port.getAfterPhotoUrl());
        r.setPhotoUrl(port.getPhotoUrl());
        r.setVideoUrl(port.getVideoUrl());
        r.setFeatured(port.isFeatured());
        r.setCreatedAt(port.getCreatedAt());
        return r;
    }

    private boolean matchesGroupBySpecialization(String specialization, String targetGroup) {
        if (specialization == null || targetGroup == null) return false;
        String spec  = specialization.toLowerCase();
        String group = targetGroup.toUpperCase();
        switch (group) {
            case "MEN":
                return spec.contains("beard") || spec.contains("shave") || spec.contains("men")
                        || spec.contains("grooming") || spec.contains("haircut")
                        || spec.contains("hair cut") || spec.contains("hair color")
                        || spec.contains("hair colour") || spec.contains("hair style");
            case "WOMEN":
                return spec.contains("bridal") || spec.contains("makeup") || spec.contains("manicure")
                        || spec.contains("pedicure") || spec.contains("facial") || spec.contains("waxing")
                        || spec.contains("threading") || spec.contains("women") || spec.contains("nail")
                        || spec.contains("hair color") || spec.contains("hair colour")
                        || spec.contains("hair style") || spec.contains("haircut");
            case "KIDS":
                return spec.contains("kids") || spec.contains("children") || spec.contains("child");
            default:
                return false;
        }
    }

    private boolean matchesCategoryBySpecialization(String specialization, String category) {
        if (specialization == null || category == null) return false;
        String spec = specialization.toLowerCase();
        String cat  = category.toLowerCase();
        if (spec.contains(cat)) return true;
        switch (cat) {
            case "beard":    return spec.contains("beard") || spec.contains("shave");
            case "skin":     return spec.contains("facial") || spec.contains("cleanup") || spec.contains("face");
            case "nails":    return spec.contains("manicure") || spec.contains("pedicure") || spec.contains("nail");
            case "makeup":   return spec.contains("makeup") || spec.contains("bridal") || spec.contains("party");
            case "body":     return spec.contains("wax") || spec.contains("thread")
                                 || spec.contains("massage") || spec.contains("body");
            case "packages": return spec.contains("package") || spec.contains("grooming");
            default:         return false;
        }
    }
}
