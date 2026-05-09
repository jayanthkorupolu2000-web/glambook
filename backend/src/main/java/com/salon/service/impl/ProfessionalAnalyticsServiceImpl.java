package com.salon.service.impl;

import com.salon.dto.response.ProfessionalAnalyticsResponse;
import com.salon.dto.response.ServicePopularityResponse;
import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import com.salon.entity.Payment;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.PaymentRepository;
import com.salon.repository.ReviewRepository;
import com.salon.service.ProfessionalAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalAnalyticsServiceImpl implements ProfessionalAnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public ProfessionalAnalyticsResponse generateAnalytics(Long professionalId) {
        ProfessionalAnalyticsResponse res = new ProfessionalAnalyticsResponse();
        res.setProfessionalId(professionalId);

        List<Appointment> all = appointmentRepository.findByProfessionalIdAndStatus(professionalId, AppointmentStatus.COMPLETED);
        res.setTotalAppointments(appointmentRepository.findByProfessionalSalonOwnerId(professionalId).size());
        res.setCompletedAppointments(appointmentRepository.countByProfessionalIdAndStatus(professionalId, AppointmentStatus.COMPLETED));
        res.setCancelledAppointments(appointmentRepository.countByProfessionalIdAndStatus(professionalId, AppointmentStatus.CANCELLED));

        BigDecimal earnings = paymentRepository.sumEarningsByProfessionalId(professionalId);
        res.setTotalEarnings(earnings != null ? earnings : BigDecimal.ZERO);

        Double avg = reviewRepository.findAverageRatingByProfessionalId(professionalId);
        res.setAverageRating(avg != null ? avg : 0.0);
        res.setTotalReviews((int) reviewRepository.countByProfessionalId(professionalId));

        // Retention rate
        Set<Long> customerIds = all.stream().map(a -> a.getCustomer().getId()).collect(Collectors.toSet());
        long repeatCustomers = customerIds.stream()
                .filter(cid -> all.stream().filter(a -> a.getCustomer().getId().equals(cid)).count() > 1)
                .count();
        res.setClientRetentionRate(customerIds.isEmpty() ? 0.0 : (double) repeatCustomers / customerIds.size() * 100);

        // Popular services
        Map<Long, Long> serviceCount = all.stream()
                .collect(Collectors.groupingBy(a -> a.getService().getId(), Collectors.counting()));
        List<ServicePopularityResponse> popular = serviceCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    ServicePopularityResponse sp = new ServicePopularityResponse();
                    sp.setServiceId(e.getKey());
                    sp.setBookingCount(e.getValue());
                    sp.setTotalRevenue(BigDecimal.ZERO);
                    all.stream().filter(a -> a.getService().getId().equals(e.getKey()))
                            .findFirst().ifPresent(a -> sp.setServiceName(a.getService().getName()));
                    return sp;
                }).collect(Collectors.toList());
        res.setPopularServices(popular);

        // Peak day
        if (!all.isEmpty()) {
            Map<String, Long> dayCount = all.stream()
                    .collect(Collectors.groupingBy(a -> a.getDateTime().getDayOfWeek().name(), Collectors.counting()));
            res.setPeakBookingDay(dayCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A"));

            Map<Integer, Long> hourCount = all.stream()
                    .collect(Collectors.groupingBy(a -> a.getDateTime().getHour(), Collectors.counting()));
            res.setPeakBookingHour(hourCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0));
        } else {
            res.setPeakBookingDay("N/A");
            res.setPeakBookingHour(0);
        }

        // Monthly earnings (last 6 months)
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--) {
            LocalDateTime from = LocalDateTime.now().minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime to = from.plusMonths(1);
            String label = from.format(fmt);
            List<Payment> payments = paymentRepository.findPaidPaymentsByProfessionalAndDateRange(professionalId, from, to);
            BigDecimal total = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            monthly.put(label, total);
        }
        res.setMonthlyEarnings(monthly);
        res.setReportGeneratedAt(LocalDateTime.now());

        return res;
    }
}
