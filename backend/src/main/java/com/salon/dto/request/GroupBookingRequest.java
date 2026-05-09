package com.salon.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class GroupBookingRequest {
    @NotNull(message = "Please provide a valid customerId")
    private Long customerId;

    private Long salonOwnerId;

    @NotNull(message = "Please provide a valid scheduledAt")
    @Future(message = "Please provide a valid scheduledAt")
    private LocalDateTime scheduledAt;

    private BigDecimal discountPct;
    private String notes;

    // List of professional IDs to book simultaneously
    @NotNull(message = "Please provide a valid professionalIds")
    private List<Long> professionalIds;

    // Optional: single serviceId fallback (used if participantServices not provided)
    private Long serviceId;

    // Per-professional service mapping: [{professionalId, serviceId}, ...]
    private List<ParticipantService> participantServices;

    @Data
    public static class ParticipantService {
        private Long professionalId;
        private Long serviceId;
    }
}
