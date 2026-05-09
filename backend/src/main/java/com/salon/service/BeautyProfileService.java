package com.salon.service;

import com.salon.dto.request.BeautyProfileRequest;
import com.salon.dto.response.BeautyProfileResponse;
import com.salon.entity.BeautyProfile;
import com.salon.entity.Customer;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.BeautyProfileRepository;
import com.salon.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeautyProfileService {

    private final BeautyProfileRepository beautyProfileRepository;
    private final CustomerRepository customerRepository;

    /**
     * Save (create or update) a customer's beauty profile.
     * Uses upsert logic: if a profile already exists for this customer, update it;
     * otherwise create a new one.
     */
    @Transactional
    public BeautyProfileResponse saveProfile(Long customerId, BeautyProfileRequest dto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        BeautyProfile profile = beautyProfileRepository.findByCustomerId(customerId)
                .orElse(BeautyProfile.builder().customer(customer).build());

        profile.setSkinType(dto.getSkinType());
        profile.setHairType(dto.getHairType());
        profile.setHairTexture(dto.getHairTexture());
        profile.setAllergies(dto.getAllergies());
        profile.setPreferredServices(dto.getPreferredServices());
        profile.setNotes(dto.getNotes());
        profile.setUpdatedAt(LocalDateTime.now());

        BeautyProfile saved = beautyProfileRepository.save(profile);
        log.info("Saved beauty profile for customer {}", customerId);
        return toResponse(saved);
    }

    /**
     * Fetch a customer's beauty profile.
     * Returns an empty response (all nulls) if no profile exists yet —
     * so the frontend form can still render without a 404.
     */
    public BeautyProfileResponse getProfile(Long customerId) {
        return beautyProfileRepository.findByCustomerId(customerId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    BeautyProfileResponse empty = new BeautyProfileResponse();
                    empty.setCustomerId(customerId);
                    return empty;
                });
    }

    private BeautyProfileResponse toResponse(BeautyProfile p) {
        BeautyProfileResponse r = new BeautyProfileResponse();
        r.setId(p.getId());
        r.setCustomerId(p.getCustomer().getId());
        r.setSkinType(p.getSkinType());
        r.setHairType(p.getHairType());
        r.setHairTexture(p.getHairTexture());
        r.setAllergies(p.getAllergies());
        r.setPreferredServices(p.getPreferredServices());
        r.setNotes(p.getNotes());
        return r;
    }
}
