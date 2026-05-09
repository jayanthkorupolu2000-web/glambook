package com.salon.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OwnerReportResponse {
    private Long ownerId;
    private long totalAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private BigDecimal totalRevenue;
    private double averageRating;
    private long totalComplaints;
    private long openComplaints;
    private long forwardedComplaints;
    private long resolvedComplaints;
    private long professionalsCount;
    private long pendingApprovals;
    private LocalDateTime reportGeneratedAt;
}
