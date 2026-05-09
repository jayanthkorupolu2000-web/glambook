package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OwnerNotificationResponse {
    private Long id;
    private String type;
    private Long referenceId;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
}
