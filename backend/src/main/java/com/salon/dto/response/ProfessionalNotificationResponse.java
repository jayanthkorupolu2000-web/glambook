package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProfessionalNotificationResponse {
    private Long id;
    private String type;
    private Long referenceId;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
}
