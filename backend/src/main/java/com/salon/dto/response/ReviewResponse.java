package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long professionalId;
    private Long appointmentId;
    private Integer rating;
    private String comment;
    private java.util.List<String> photos;
    private String professionalResponse;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
