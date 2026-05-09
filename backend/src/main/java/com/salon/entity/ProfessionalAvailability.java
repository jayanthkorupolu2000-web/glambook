package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "professional_availability")
public class ProfessionalAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Column(name = "avail_date", nullable = false)
    private LocalDate availDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(name = "is_booked")
    private boolean isBooked = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "slot_type")
    private SlotType slotType = SlotType.WORKING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "slot_status", columnDefinition = "VARCHAR(20)")
    private SlotStatus slotStatus = SlotStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
