package com.salon.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupBookingResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long salonOwnerId;
    private String salonOwnerName;
    private LocalDateTime scheduledAt;
    private BigDecimal discountPct;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private List<AppointmentResponse> appointments;
}
