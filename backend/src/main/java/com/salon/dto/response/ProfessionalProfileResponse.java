package com.salon.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProfessionalProfileResponse {
    private Long id;
    private Long professionalId;   // alias of id, used by search result cards
    private String name;
    private String email;
    private String city;
    private String cityName;       // alias of city, used by search result cards
    private String specialization;
    private Integer experienceYears;
    private String certifications;
    private String trainingDetails;
    private String serviceAreas;
    private Integer travelRadiusKm;
    private String bio;
    private String instagramHandle;
    @JsonProperty("isAvailableHome")
    private boolean isAvailableHome;
    @JsonProperty("isAvailableSalon")
    private boolean isAvailableSalon;
    private Integer responseTimeHrs;
    private String profilePhotoUrl;
    private String status;
    private String salonOwnerName;
    private LocalDateTime approvedAt;
    private double averageRating;
    private int totalReviews;
    private List<PortfolioResponse> featuredPortfolioItems;
    private List<ServiceSummary> activeServices;
    private List<Object> activePromotions;

    @Data
    public static class ServiceSummary {
        private Long id;
        private String name;
        private String category;
        private java.math.BigDecimal price;
        private Integer durationMins;
        private String gender;
    }
}
