package com.salon.service;

import com.salon.dto.response.CustomerResponse;
import com.salon.dto.response.ProfessionalResponse;
import com.salon.dto.response.SalonOwnerResponse;
import com.salon.entity.Customer;
import com.salon.entity.Professional;
import com.salon.entity.SalonOwner;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.CustomerRepository;
import com.salon.repository.PaymentRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.repository.SalonOwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final CustomerRepository customerRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Returns all users across all roles: customers, salon owners, and professionals.
     * Validates: Property 19 — user list contains all registered users across all roles.
     */
    public Map<String, Object> getAllUsers() {
        log.info("Fetching all users across all roles");

        List<CustomerResponse> customers = customerRepository.findAll()
                .stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());

        List<SalonOwnerResponse> owners = salonOwnerRepository.findAll()
                .stream()
                .map(this::mapToSalonOwnerResponse)
                .collect(Collectors.toList());

        List<ProfessionalResponse> professionals = professionalRepository.findAll()
                .stream()
                .map(this::mapToProfessionalResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("customers", customers);
        result.put("owners", owners);
        result.put("professionals", professionals);
        result.put("totalCount", customers.size() + owners.size() + professionals.size());

        log.info("Fetched {} customers, {} owners, {} professionals",
                customers.size(), owners.size(), professionals.size());
        return result;
    }

    /**
     * Returns all pre-seeded salon owners.
     */
    public List<SalonOwnerResponse> getAllOwners() {
        log.info("Fetching all salon owners");
        return salonOwnerRepository.findAll()
                .stream()
                .map(this::mapToSalonOwnerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a summary report of appointments and payments.
     */
    public Map<String, Object> getReports() {
        log.info("Generating admin reports");

        long totalAppointments = appointmentRepository.count();
        long totalPayments = paymentRepository.count();

        Map<String, Object> report = new HashMap<>();
        report.put("totalAppointments", totalAppointments);
        report.put("totalPayments", totalPayments);

        log.info("Report: {} appointments, {} payments", totalAppointments, totalPayments);
        return report;
    }

    private CustomerResponse mapToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .city(customer.getCity())
                .status(customer.getStatus() != null ? customer.getStatus().name() : "ACTIVE")
                .cancelCount(customer.getCancelCount())
                .build();
    }

    private SalonOwnerResponse mapToSalonOwnerResponse(SalonOwner owner) {
        return SalonOwnerResponse.builder()
                .id(owner.getId())
                .name(owner.getName())
                .salonName(owner.getSalonName())
                .city(owner.getCity())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .build();
    }

    private ProfessionalResponse mapToProfessionalResponse(Professional professional) {
        SalonOwnerResponse ownerResponse = professional.getSalonOwner() != null
                ? mapToSalonOwnerResponse(professional.getSalonOwner())
                : null;
        return ProfessionalResponse.builder()
                .id(professional.getId())
                .name(professional.getName())
                .email(professional.getEmail())
                .city(professional.getCity())
                .specialization(professional.getSpecialization())
                .experienceYears(professional.getExperienceYears())
                .status(professional.getStatus() != null ? professional.getStatus().name() : "ACTIVE")
                .salonOwner(ownerResponse)
                .build();
    }
}
