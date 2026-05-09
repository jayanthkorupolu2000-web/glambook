package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private PortfolioMediaType mediaType;

    @Column(name = "before_photo_url")
    private String beforePhotoUrl;

    @Column(name = "after_photo_url")
    private String afterPhotoUrl;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "video_thumbnail")
    private String videoThumbnail;

    @Column(name = "service_tag")
    private String serviceTag;

    /** Relative path to a file in the frontend assets folder, e.g. /assets/portfolio/haircut1.jpg */
    @Column(name = "file_path")
    private String filePath;

    /** Comma-separated tags, e.g. "Haircut,Men,Short" */
    @Column(name = "tags")
    private String tags;

    private String caption;

    @Column(columnDefinition = "TEXT")
    private String testimonial;

    @Column(name = "is_featured")
    @Builder.Default
    private boolean isFeatured = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
