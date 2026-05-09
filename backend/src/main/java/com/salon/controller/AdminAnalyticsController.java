package com.salon.controller;

import com.salon.dto.response.CancellationStatsResponse;
import com.salon.dto.response.PaymentStatsResponse;
import com.salon.entity.PaymentStatus;
import com.salon.entity.UserStatus;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.ComplaintRepository;
import com.salon.repository.CustomerRepository;
import com.salon.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Analytics", description = "Analytics and reporting APIs for admin")
public class AdminAnalyticsController {

    private final ComplaintRepository complaintRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    @GetMapping("/complaints-by-city")
    @Operation(summary = "Complaints per city", description = "Returns complaint count grouped by city")
    public ResponseEntity<Map<String, Long>> complaintsByCity() {
        Map<String, Long> result = new LinkedHashMap<>();
        complaintRepository.countComplaintsByCity()
                .forEach(row -> result.put((String) row[0], (Long) row[1]));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ratings-distribution")
    @Operation(summary = "Ratings distribution", description = "Returns complaint count grouped by rating value")
    public ResponseEntity<Map<Integer, Long>> ratingsDistribution() {
        Map<Integer, Long> result = new LinkedHashMap<>();
        complaintRepository.countByRating()
                .forEach(row -> result.put((Integer) row[0], (Long) row[1]));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/cancellations")
    @Operation(summary = "Cancellation stats", description = "Returns total, same-day cancellations and suspended customers")
    public ResponseEntity<CancellationStatsResponse> cancellationStats() {
        long total = appointmentRepository.countByCancelledAtIsNotNull();
        long sameDay = appointmentRepository.countSameDayCancellations();
        long suspended = customerRepository.countByStatus(UserStatus.SUSPENDED);
        return ResponseEntity.ok(new CancellationStatsResponse(total, sameDay, suspended));
    }

    @GetMapping("/payments")
    @Operation(summary = "Payment stats", description = "Returns total, paid, refunded payments and success ratio")
    public ResponseEntity<PaymentStatsResponse> paymentStats() {
        long total = paymentRepository.count();
        long paid = paymentRepository.countByStatus(PaymentStatus.PAID);
        long refunded = paymentRepository.countByStatus(PaymentStatus.REFUNDED);
        double ratio = total > 0 ? (double) paid / total * 100 : 0;
        return ResponseEntity.ok(new PaymentStatsResponse(total, paid, refunded, Math.round(ratio * 10.0) / 10.0));
    }
}
