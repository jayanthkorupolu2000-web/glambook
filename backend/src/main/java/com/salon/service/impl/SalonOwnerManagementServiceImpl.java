package com.salon.service.impl;

import com.salon.dto.request.SalonOwnerManagementEditRequest;
import com.salon.dto.response.SalonOwnerManagementResponse;
import com.salon.entity.SalonOwner;
import com.salon.exception.SalonOwnerNotFoundException;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.SalonOwnerManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the business logic for editing Salon Owner details.
 * Only ownerName (name), salonName, and phone are mutated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalonOwnerManagementServiceImpl implements SalonOwnerManagementService {

    private final SalonOwnerRepository salonOwnerRepository;

    @Override
    @Transactional
    public SalonOwnerManagementResponse updateSalonOwner(Long id, SalonOwnerManagementEditRequest dto) {
        log.info("Admin updating SalonOwner id={}", id);

        SalonOwner owner = salonOwnerRepository.findById(id)
                .orElseThrow(() -> new SalonOwnerNotFoundException(id));

        // Only mutate the three allowed fields
        owner.setName(dto.getOwnerName());
        owner.setSalonName(dto.getSalonName());
        owner.setPhone(dto.getPhone());

        SalonOwner saved = salonOwnerRepository.save(owner);
        log.info("SalonOwner id={} updated: name='{}', salonName='{}', phone='{}'",
                id, saved.getName(), saved.getSalonName(), saved.getPhone());

        return toResponse(saved);
    }

    private SalonOwnerManagementResponse toResponse(SalonOwner owner) {
        return SalonOwnerManagementResponse.builder()
                .id(owner.getId())
                .ownerName(owner.getName())
                .salonName(owner.getSalonName())
                .city(owner.getCity())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .build();
    }
}
