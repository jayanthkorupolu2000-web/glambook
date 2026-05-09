package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    /** Overall rating (auto-computed as average of the three criteria) */
    private Integer rating;

    /** Quality of the service (1–5) */
    @Column(name = "quality_rating")
    private Integer qualityRating;

    /** Timeliness / punctuality (1–5) */
    @Column(name = "timeliness_rating")
    private Integer timelinessRating;

    /** Professionalism of the professional (1–5) */
    @Column(name = "professionalism_rating")
    private Integer professionalismRating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** Stored as JSON array of file paths, e.g. ["/uploads/reviews/abc.jpg"] */
    @Column(columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @Column(name = "professional_response", columnDefinition = "TEXT")
    private String professionalResponse;

    @Column(name = "professional_response_at")
    private LocalDateTime professionalResponseAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.ACTIVE;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
