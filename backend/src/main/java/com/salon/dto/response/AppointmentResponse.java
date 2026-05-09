package com.salon.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AppointmentResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long serviceId;
    private String serviceName;
    private String serviceCategory;
    private java.math.BigDecimal servicePrice;
    private Long professionalId;
    private String professionalName;
    private String professionalPhotoUrl;
    private String salonOwnerName;
    private String salonCity;
    private LocalDateTime scheduledAt;
    private String type;
    private String status;
    private BigDecimal travelFee;
    private String homeAddress;
    private String homeAccessNotes;
    private BigDecimal totalAmount;
    private Long groupBookingId;
    private Long rebookedFromId;
    private List<PaymentResponse> payments;
    private boolean canReview;
    private boolean canRebook;
    private boolean canCancel;
}
