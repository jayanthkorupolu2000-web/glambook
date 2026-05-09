package com.salon.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ServicePopularityResponse {
    private Long serviceId;
    private String serviceName;
    private long bookingCount;
    private BigDecimal totalRevenue;
}
