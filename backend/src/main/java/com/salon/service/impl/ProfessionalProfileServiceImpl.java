package com.salon.service.impl;

import com.salon.dto.request.ProfessionalProfileUpdateRequest;
import com.salon.dto.response.PortfolioResponse;
import com.salon.dto.response.ProfessionalProfileResponse;
import com.salon.entity.Professional;
import com.salon.entity.UserStatus;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.PortfolioRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.ReviewRepository;
import com.salon.repository.ServiceRepository;
import com.salon.service.FileStorageService;
import com.salon.service.ProfessionalProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Override
    @Transactional
    public ProfessionalProfileResponse updateProfile(Long professionalId, ProfessionalProfileUpdateRequest dto) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));

        if (professional.getStatus() == UserStatus.SUSPENDED) {
            throw new InvalidOperationException("Suspended professionals cannot update their profile");
        }

        professional.setSpecialization(dto.getSpecialization());
        professional.setExperienceYears(dto.getExperienceYears());
        if (dto.getCertifications() != null) professional.setCertifications(dto.getCertifications());
        if (dto.getTrainingDetails() != null) professional.setTrainingDetails(dto.getTrainingDetails());
        if (dto.getServiceAreas() != null) professional.setServiceAreas(dto.getServiceAreas());
        if (dto.getTravelRadiusKm() != null) professional.setTravelRadiusKm(dto.getTravelRadiusKm());
        if (dto.getBio() != null) professional.setBio(dto.getBio());
        if (dto.getInstagramHandle() != null) professional.setInstagramHandle(dto.getInstagramHandle());
        if (dto.getIsAvailableHome() != null) professional.setAvailableHome(dto.getIsAvailableHome());
        if (dto.getIsAvailableSalon() != null) professional.setAvailableSalon(dto.getIsAvailableSalon());
        if (dto.getResponseTimeHrs() != null) professional.setResponseTimeHrs(dto.getResponseTimeHrs());

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
    public List<ProfessionalProfileResponse> searchProfessionals(String city, String targetGroup, String category, Boolean homeAvailable) {
        return searchProfessionalsWithPrice(city, targetGroup, category, homeAvailable, null, null, null, null);
    }

    @Override
    public List<ProfessionalProfileResponse> searchProfessionalsWithPrice(
            String city, String targetGroup, String category,
            Boolean homeAvailable, String keyword,
            Double minPrice, Double maxPrice, Double minRating) {

        return professionalRepository.findAll().stream()
                .filter(p -> p.getStatus() == UserStatus.ACTIVE || p.getStatus() == null)
                .filter(p -> city == null || city.isBlank() || p.getCity().equalsIgnoreCase(city))
                .filter(p -> homeAvailable == null || !homeAvailable || p.isAvailableHome())
                .filter(p -> keyword == null || keyword.isBlank()
                        || (p.getSpecialization() != null && p.getSpecialization().toLowerCase().contains(keyword.toLowerCase()))
                        || p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .map(this::toResponse)
                // Price range: keep professional if ANY of their active services falls in range
                .filter(res -> {
                    if ((minPrice == null || minPrice <= 0) && (maxPrice == null || maxPrice <= 0)) return true;
                    if (res.getActiveServices() == null || res.getActiveServices().isEmpty()) return false;
                    return res.getActiveServices().stream().anyMatch(svc -> {
                        if (svc.getPrice() == null) return false;
                        double p = svc.getPrice().doubleValue();
                        boolean aboveMin = (minPrice == null || minPrice <= 0) || p >= minPrice;
                        boolean belowMax = (maxPrice == null || maxPrice <= 0) || p <= maxPrice;
                        return aboveMin && belowMax;
                    });
                })
                // Rating filter
                .filter(res -> minRating == null || minRating <= 0 || res.getAverageRating() >= minRating)
                .collect(java.util.stream.Collectors.toList());
    }

    private ProfessionalProfileResponse toResponse(Professional p) {
        ProfessionalProfileResponse res = new ProfessionalProfileResponse();
        res.setId(p.getId());
        res.setProfessionalId(p.getId()); // alias used by search results frontend
        res.setName(p.getName());
        res.setEmail(p.getEmail());
        res.setCity(p.getCity());
        res.setCityName(p.getCity());     // alias used by search results frontend
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
                    .stream().limit(3).map(this::toPortfolioResponse).collect(Collectors.toList());
            res.setFeaturedPortfolioItems(featured);
        } catch (Exception e) {
            res.setFeaturedPortfolioItems(java.util.Collections.emptyList());
        }

        // Populate active services (used for price range filtering and display)
        try {
            List<ProfessionalProfileResponse.ServiceSummary> services = serviceRepository
                    .findByProfessionalId(p.getId())
                    .stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .map(s -> {
                        ProfessionalProfileResponse.ServiceSummary ss = new ProfessionalProfileResponse.ServiceSummary();
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
}
