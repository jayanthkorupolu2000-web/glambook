package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewWithResponseDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long professionalId;
    private Long appointmentId;
    private Integer rating;
    private String comment;
    private java.util.List<String> photos;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String professionalResponse;
    private LocalDateTime professionalResponseAt;
}
