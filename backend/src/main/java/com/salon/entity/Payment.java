package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Deadline by which Pay Later must be settled (now + 24h at opt-in time). */
    @Column(name = "pay_later_deadline")
    private LocalDateTime payLaterDeadline;

    /** Number of Pay Later reminders sent (0–2 before auto-suspension). */
    @Column(name = "pay_later_reminder_count")
    @Builder.Default
    private Integer payLaterReminderCount = 0;
}
