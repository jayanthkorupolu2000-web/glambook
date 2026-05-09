package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loyalty")
public class Loyalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private SalonOwner owner;

    @Builder.Default
    private int points = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(columnDefinition = "VARCHAR(20)")
    private LoyaltyTier tier = LoyaltyTier.BRONZE;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
