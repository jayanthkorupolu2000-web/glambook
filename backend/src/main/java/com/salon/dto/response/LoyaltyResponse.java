package com.salon.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LoyaltyResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long ownerId;
    private int points;
    private String tier;
    private int totalEarned;
    private int totalRedeemed;
    private List<String> tierBenefits;
    private List<String> earlyAccessServices;
    /** Current wallet balance in ₹ (populated on summary and redeem calls) */
    private BigDecimal walletBalance;
    /** Amount credited to wallet in the most recent redemption (populated on redeem only) */
    private BigDecimal lastCreditedAmount;
}
