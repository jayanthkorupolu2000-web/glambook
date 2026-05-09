package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ServiceBeforeAfterResponse {
    private Long id;
    private Long serviceId;
    private String beforePhotoUrl;
    private String afterPhotoUrl;
    private String caption;
    private LocalDateTime uploadedAt;
}
