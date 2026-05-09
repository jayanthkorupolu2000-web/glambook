package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private BigDecimal price;

    @Column(name = "duration_mins")
    private Integer durationMins;

    @Column(name = "professional_id")
    private Long professionalId;

    @Column(name = "is_active", columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "discount_pct", columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal discountPct = BigDecimal.ZERO;
}
