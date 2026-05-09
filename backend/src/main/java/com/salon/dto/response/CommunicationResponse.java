package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommunicationResponse {
    private Long id;
    private Long professionalId;
    private Long customerId;
    private String customerName;
    private Long appointmentId;
    private String message;
    private String type;
    private boolean isRead;
    private LocalDateTime createdAt;
}
