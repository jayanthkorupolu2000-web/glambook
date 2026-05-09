package com.salon.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoyaltyTransactionResponse {
    private Long id;
    private String type;       // EARN or REDEEM
    private int points;
    private String description;
    private LocalDateTime createdAt;
}
