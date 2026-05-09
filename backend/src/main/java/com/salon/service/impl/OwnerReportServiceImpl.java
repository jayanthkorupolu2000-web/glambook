package com.salon.service.impl;

import com.salon.dto.response.OwnerReportResponse;
import com.salon.entity.AppointmentStatus;
import com.salon.entity.ComplaintStatus;
import com.salon.entity.UserStatus;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.ComplaintRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.service.OwnerReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OwnerReportServiceImpl implements OwnerReportService {

    private final AppointmentRepository appointmentRepository;
    private final ComplaintRepository complaintRepository;
    private final ProfessionalRepository professionalRepository;

    @Override
    public OwnerReportResponse generateReport(Long ownerId) {
        OwnerReportResponse report = new OwnerReportResponse();
        report.setOwnerId(ownerId);

        report.setTotalAppointments(appointmentRepository.findByProfessionalSalonOwnerId(ownerId).size());
        report.setCompletedAppointments(appointmentRepository.countByProfessionalSalonOwnerIdAndStatus(ownerId, AppointmentStatus.COMPLETED));
        report.setCancelledAppointments(appointmentRepository.countByProfessionalSalonOwnerIdAndStatus(ownerId, AppointmentStatus.CANCELLED));

        BigDecimal revenue = appointmentRepository.sumRevenueByOwnerId(ownerId);
        report.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        report.setAverageRating(0.0); // Can be extended with review repo

        report.setTotalComplaints(complaintRepository.findByProfessionalSalonOwnerId(ownerId).size());
        report.setOpenComplaints(complaintRepository.countByProfessionalSalonOwnerIdAndStatus(ownerId, ComplaintStatus.OPEN));
        report.setForwardedComplaints(complaintRepository.countByProfessionalSalonOwnerIdAndStatus(ownerId, ComplaintStatus.FORWARDED));
        report.setResolvedComplaints(complaintRepository.countByProfessionalSalonOwnerIdAndStatus(ownerId, ComplaintStatus.RESOLVED));

        report.setProfessionalsCount(professionalRepository.countBySalonOwnerIdAndStatus(ownerId, UserStatus.ACTIVE));
        report.setPendingApprovals(professionalRepository.findBySalonOwnerId(ownerId).stream()
                .filter(p -> p.getStatus() == UserStatus.ACTIVE).count()); // simplified

        report.setReportGeneratedAt(LocalDateTime.now());
        return report;
    }
}
