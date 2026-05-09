package com.salon.service.impl;

import com.salon.dto.request.PromotionRequest;
import com.salon.dto.response.PromotionResponse;
import com.salon.entity.Promotion;
import com.salon.entity.SalonOwner;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.PromotionRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final SalonOwnerRepository salonOwnerRepository;

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new InvalidOperationException("End date must be on or after start date");
        }

        SalonOwner owner = salonOwnerRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + dto.getOwnerId()));

        Promotion promotion = Promotion.builder()
                .owner(owner)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .discountPct(dto.getDiscountPct())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        return toResponse(promotionRepository.save(promotion));
    }

    @Override
    public List<PromotionResponse> getPromotionsByOwner(Long ownerId) {
        return promotionRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<PromotionResponse> getActivePromotionsByOwner(Long ownerId) {
        return promotionRepository.findActivePromotionsByOwner(ownerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deactivateExpiredPromotions() {
        List<Promotion> expired = promotionRepository.findExpiredActivePromotions();
        expired.forEach(p -> p.setActive(false));
        promotionRepository.saveAll(expired);
        log.info("Deactivated {} expired promotions", expired.size());
    }

    private PromotionResponse toResponse(Promotion p) {
        PromotionResponse res = new PromotionResponse();
        res.setId(p.getId());
        res.setOwnerId(p.getOwner().getId());
        res.setTitle(p.getTitle());
        res.setDescription(p.getDescription());
        res.setDiscountPct(p.getDiscountPct());
        res.setStartDate(p.getStartDate());
        res.setEndDate(p.getEndDate());
        res.setActive(p.isActive());
        return res;
    }
}
