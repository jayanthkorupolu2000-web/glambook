package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "beauty_profile")
public class BeautyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "skin_type")
    private String skinType;

    @Column(name = "hair_type")
    private String hairType;

    @Column(name = "hair_condition")
    private String hairCondition;

    @Column(name = "hair_texture")
    private String hairTexture;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "preferred_services", columnDefinition = "TEXT")
    private String preferredServices;

    @Column(name = "beauty_goals", columnDefinition = "TEXT")
    private String beautyGoals;

    @Column(name = "consultation_photo_url")
    private String consultationPhotoUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
