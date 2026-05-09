package com.salon.service;

import com.salon.dto.request.ProfessionalProfileUpdateRequest;
import com.salon.dto.response.ProfessionalProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProfessionalProfileService {
    ProfessionalProfileResponse updateProfile(Long professionalId, ProfessionalProfileUpdateRequest dto);
    ProfessionalProfileResponse getProfileById(Long professionalId);
    String uploadProfilePhoto(Long professionalId, MultipartFile file);
    List<ProfessionalProfileResponse> searchProfessionals(String city, String targetGroup, String category, Boolean homeAvailable);

    List<ProfessionalProfileResponse> searchProfessionalsWithPrice(
            String city, String targetGroup, String category,
            Boolean homeAvailable, String keyword,
            Double minPrice, Double maxPrice, Double minRating);
}
