package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationResponse {
    private Long id;
    private String message;
    private Long referenceId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
