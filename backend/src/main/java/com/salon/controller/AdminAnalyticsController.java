package com.salon.controller;

import com.salon.dto.response.CancellationStatsResponse;
import com.salon.dto.response.PaymentStatsResponse;
import com.salon.entity.AppointmentStatus;
import com.salon.entity.PaymentStatus;
import com.salon.entity.UserStatus;
import com.salon.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
    private final SalonOwnerRepository salonOwnerRepository;
    private final ProfessionalRepository professionalRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping("/complaints-by-city")
    @Operation(summary = "Complaints per city")
    public ResponseEntity<Map<String, Long>> complaintsByCity() {
        Map<String, Long> result = new LinkedHashMap<>();
        complaintRepository.countComplaintsByCity()
                .forEach(row -> result.put((String) row[0], (Long) row[1]));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ratings-distribution")
    @Operation(summary = "Ratings distribution")
    public ResponseEntity<Map<Integer, Long>> ratingsDistribution() {
        Map<Integer, Long> result = new LinkedHashMap<>();
        complaintRepository.countByRating()
                .forEach(row -> result.put((Integer) row[0], (Long) row[1]));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/cancellations")
    @Operation(summary = "Cancellation stats")
    public ResponseEntity<CancellationStatsResponse> cancellationStats() {
        long total = appointmentRepository.countByCancelledAtIsNotNull();
        long sameDay = appointmentRepository.countSameDayCancellations();
        long suspended = customerRepository.countByStatus(UserStatus.SUSPENDED);
        return ResponseEntity.ok(new CancellationStatsResponse(total, sameDay, suspended));
    }

    @GetMapping("/payments")
    @Operation(summary = "Payment stats")
    public ResponseEntity<PaymentStatsResponse> paymentStats() {
        long total = paymentRepository.count();
        long paid = paymentRepository.countByStatus(PaymentStatus.PAID);
        long refunded = paymentRepository.countByStatus(PaymentStatus.REFUNDED);
        double ratio = total > 0 ? (double) paid / total * 100 : 0;
        return ResponseEntity.ok(new PaymentStatsResponse(total, paid, refunded, Math.round(ratio * 10.0) / 10.0));
    }

    /**
     * Per-salon analytics: revenue, appointments, ratings, professionals breakdown.
     * Returns a list of salon summaries with nested professional stats.
     */
    @GetMapping("/salons")
    @Operation(summary = "Per-salon analytics with professional breakdown")
    public ResponseEntity<List<Map<String, Object>>> salonAnalytics() {
        List<Map<String, Object>> result = salonOwnerRepository.findAll().stream().map(owner -> {
            Map<String, Object> salon = new LinkedHashMap<>();
            salon.put("ownerId",    owner.getId());
            salon.put("ownerName",  owner.getName());
            salon.put("salonName",  owner.getSalonName());
            salon.put("city",       owner.getCity());

            // Appointments
            long total     = appointmentRepository.findByProfessionalSalonOwnerId(owner.getId()).size();
            long completed = appointmentRepository.countByProfessionalSalonOwnerIdAndStatus(owner.getId(), AppointmentStatus.COMPLETED);
            long cancelled = appointmentRepository.countByProfessionalSalonOwnerIdAndStatus(owner.getId(), AppointmentStatus.CANCELLED);
            salon.put("totalAppointments",     total);
            salon.put("completedAppointments", completed);
            salon.put("cancelledAppointments", cancelled);

            // Revenue
            BigDecimal revenue = appointmentRepository.sumRevenueByOwnerId(owner.getId());
            salon.put("totalRevenue", revenue != null ? revenue : BigDecimal.ZERO);

            // Professionals
            List<Map<String, Object>> profList = professionalRepository
                    .findBySalonOwnerId(owner.getId()).stream().map(prof -> {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("professionalId",   prof.getId());
                p.put("name",             prof.getName());
                p.put("specialization",   prof.getSpecialization());
                p.put("status",           prof.getStatus() != null ? prof.getStatus().name() : "ACTIVE");

                // Per-professional appointments
                long pCompleted = appointmentRepository.countByProfessionalIdAndStatus(prof.getId(), AppointmentStatus.COMPLETED);
                long pCancelled = appointmentRepository.countByProfessionalIdAndStatus(prof.getId(), AppointmentStatus.CANCELLED);
                long pTotal     = pCompleted + pCancelled
                        + appointmentRepository.countByProfessionalIdAndStatus(prof.getId(), AppointmentStatus.CONFIRMED)
                        + appointmentRepository.countByProfessionalIdAndStatus(prof.getId(), AppointmentStatus.PENDING);
                p.put("totalAppointments",     pTotal);
                p.put("completedAppointments", pCompleted);
                p.put("cancelledAppointments", pCancelled);

                // Per-professional revenue
                BigDecimal pRevenue = paymentRepository.sumEarningsByProfessionalId(prof.getId());
                p.put("revenue", pRevenue != null ? pRevenue : BigDecimal.ZERO);

                // Per-professional rating
                Double avgRating = reviewRepository.findAverageRatingByProfessionalId(prof.getId());
                long reviewCount = reviewRepository.countByProfessionalId(prof.getId());
                p.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
                p.put("totalReviews",  reviewCount);

                return p;
            }).collect(Collectors.toList());

            salon.put("professionals", profList);
            salon.put("professionalsCount", profList.size());

            // Salon-level average rating (average of all professionals)
            double salonAvgRating = profList.stream()
                    .mapToDouble(p -> ((Number) p.get("averageRating")).doubleValue())
                    .filter(r -> r > 0).average().orElse(0.0);
            salon.put("averageRating", Math.round(salonAvgRating * 10.0) / 10.0);

            // Total reviews across all professionals
            long totalReviews = profList.stream()
                    .mapToLong(p -> ((Number) p.get("totalReviews")).longValue()).sum();
            salon.put("totalReviews", totalReviews);

            return salon;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * System-wide summary KPIs for the top of the analytics page.
     */
    @GetMapping("/summary")
    @Operation(summary = "System-wide KPI summary")
    public ResponseEntity<Map<String, Object>> summary() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalAppointments",  appointmentRepository.count());
        s.put("totalPayments",      paymentRepository.count());
        s.put("totalRevenue",       paymentRepository.sumTotalRevenue());
        s.put("totalCustomers",     customerRepository.count());
        s.put("suspendedCustomers", customerRepository.countByStatus(UserStatus.SUSPENDED));
        s.put("totalSalons",        salonOwnerRepository.count());
        s.put("totalProfessionals", professionalRepository.count());
        long paid = paymentRepository.countByStatus(PaymentStatus.PAID);
        long total = paymentRepository.count();
        s.put("paymentSuccessRate", total > 0 ? Math.round((double) paid / total * 1000.0) / 10.0 : 0.0);
        return ResponseEntity.ok(s);
    }
}
