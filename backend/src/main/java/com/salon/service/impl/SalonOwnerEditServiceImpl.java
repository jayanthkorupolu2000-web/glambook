package com.salon.service.impl;

import com.salon.dto.request.SalonOwnerEditRequest;
import com.salon.dto.response.SalonOwnerEditResponse;
import com.salon.entity.SalonOwner;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.SalonOwnerEditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of SalonOwnerEditService.
 * Only name and phone are mutated — all other fields are left untouched.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalonOwnerEditServiceImpl implements SalonOwnerEditService {

    private final SalonOwnerRepository salonOwnerRepository;

    @Override
    @Transactional
    public SalonOwnerEditResponse updateSalonOwner(Long id, SalonOwnerEditRequest dto) {
        log.info("Admin updating SalonOwner id={} name='{}' phone='{}'", id, dto.getName(), dto.getPhone());

        SalonOwner owner = salonOwnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon Owner not found with id: " + id));

        // Only update the two allowed fields
        owner.setName(dto.getName());
        owner.setPhone(dto.getPhone());

        SalonOwner saved = salonOwnerRepository.save(owner);
        log.info("SalonOwner id={} updated successfully", id);

        return toResponse(saved);
    }

    private SalonOwnerEditResponse toResponse(SalonOwner owner) {
        return SalonOwnerEditResponse.builder()
                .id(owner.getId())
                .name(owner.getName())
                .phone(owner.getPhone())
                .email(owner.getEmail())
                .city(owner.getCity())
                .role("SALON_OWNER")
                .additionalInfo(owner.getSalonName())
                .build();
    }
}
