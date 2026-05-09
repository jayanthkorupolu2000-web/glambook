package com.salon.service;

import com.salon.dto.request.PromotionRequest;
import com.salon.dto.response.PromotionResponse;

import java.util.List;

public interface PromotionService {
    PromotionResponse createPromotion(PromotionRequest dto);
    List<PromotionResponse> getPromotionsByOwner(Long ownerId);
    List<PromotionResponse> getActivePromotionsByOwner(Long ownerId);
    void deactivateExpiredPromotions();
}
