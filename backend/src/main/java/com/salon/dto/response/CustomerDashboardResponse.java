package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerDashboardResponse {
    private String customerName;
    private String profilePhotoUrl;
    private String cityName;
    private LocalDateTime memberSince;
    private List<AppointmentResponse> upcomingAppointments;
    private List<AppointmentResponse> pendingAppointments;
    private List<AppointmentResponse> pendingReviews;
    private int totalLoyaltyPoints;
    private int unreadNotificationCount;
    private int pendingOrderCount;
    private boolean beautyProfileComplete;
    private PolicyResponse latestGlobalPolicy;
}
