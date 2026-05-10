package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalResponse {
    private Long id;
    private String name;
    private String email;
    private String city;
    private String specialization;
    private Integer experienceYears;
    private SalonOwnerResponse salonOwner;
    private List<ServiceResponse> services;
    private Double rating;
    private String status;
    private String suspensionReason;
    private LocalDateTime suspendedUntil;
}
