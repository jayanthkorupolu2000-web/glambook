package com.salon.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProfessionalAnalyticsResponse {
    private Long professionalId;
    private long totalAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private BigDecimal totalEarnings;
    private double averageRating;
    private int totalReviews;
    private double clientRetentionRate;
    private List<ServicePopularityResponse> popularServices;
    private String peakBookingDay;
    private int peakBookingHour;
    private Map<String, BigDecimal> monthlyEarnings;
    private LocalDateTime reportGeneratedAt;
}
