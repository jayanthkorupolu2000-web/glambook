package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Professional", indexes = {
        @Index(name = "idx_professional_city", columnList = "city")
})
public class Professional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;
    private String city;
    private String specialization;

    @Builder.Default
    private Integer experienceYears = 0;

    @ManyToOne
    @JoinColumn(name = "salon_owner_id")
    private SalonOwner salonOwner;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private SalonOwner approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Media and discovery fields
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String certifications;

    @Column(name = "training_details", columnDefinition = "TEXT")
    private String trainingDetails;

    @Column(name = "service_areas", columnDefinition = "TEXT")
    private String serviceAreas;

    @Column(name = "travel_radius_km")
    @Builder.Default
    private Integer travelRadiusKm = 0;

    @Column(name = "response_time_hrs")
    @Builder.Default
    private Integer responseTimeHrs = 24;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "instagram_handle")
    private String instagramHandle;

    @Column(name = "is_available_home")
    @Builder.Default
    private boolean isAvailableHome = false;

    @Column(name = "is_available_salon")
    @Builder.Default
    private boolean isAvailableSalon = true;
}
