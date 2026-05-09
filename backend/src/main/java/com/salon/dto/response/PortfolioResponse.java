package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PortfolioResponse {
    private Long id;
    private Long professionalId;
    private String professionalName;
    private Long serviceId;
    private String serviceTag;
    private String filePath;
    private String tags;
    private String mediaType;
    private String beforePhotoUrl;
    private String afterPhotoUrl;
    private String photoUrl;
    private String videoUrl;
    private String videoThumbnail;
    private String caption;
    private String testimonial;
    private boolean isFeatured;
    private LocalDateTime createdAt;
}
