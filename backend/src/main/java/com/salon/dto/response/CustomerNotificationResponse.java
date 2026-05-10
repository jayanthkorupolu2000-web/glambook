package com.salon.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CustomerNotificationResponse {
    private Long id;
    private String type;
    private Long referenceId;
    private String message;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdAt;
}
