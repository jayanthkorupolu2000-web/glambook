package com.salon.service;

import com.salon.dto.response.LoyaltyResponse;
import com.salon.dto.response.LoyaltyTransactionResponse;
import com.salon.entity.LoyaltyTier;

import java.util.List;

public interface LoyaltyService {
    LoyaltyResponse updatePoints(Long customerId, Long ownerId, Integer pointsToAdd);
    LoyaltyResponse getLoyaltyByCustomer(Long customerId, Long ownerId);
    List<LoyaltyResponse> getAllLoyaltyByOwner(Long ownerId);
    LoyaltyTier calculateTier(int points);
    void awardPointsOnAppointmentCompletion(Long appointmentId);

    /** Customer-facing: aggregate points across all owners */
    LoyaltyResponse getSummary(Long customerId);

    /** Redeem points for a discount */
    LoyaltyResponse redeemPoints(Long customerId, int pointsToRedeem);

    /** Full earn/redeem transaction history */
    List<LoyaltyTransactionResponse> getTransactions(Long customerId);
}
