package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentStatsResponse {
    private long total;
    private long paid;
    private long refunded;
    private double successRatio;
}
