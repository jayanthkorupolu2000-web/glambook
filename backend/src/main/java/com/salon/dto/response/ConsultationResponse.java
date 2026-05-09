package com.salon.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long professionalId;
    private String professionalName;
    private String type;
    private String topic;
    private String question;
    private String notes;
    private String photoUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
