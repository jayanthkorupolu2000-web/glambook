package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplaintResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long professionalId;
    private String professionalName;
    private String description;
    private String feedback;
    private Integer rating;
    private String status;
    private String resolutionNotes;
    private LocalDateTime createdAt;
}
