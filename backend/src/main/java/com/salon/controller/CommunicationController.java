package com.salon.controller;

import com.salon.entity.*;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/professionals/{professionalId}/communications")
@RequiredArgsConstructor
@Tag(name = "Communications", description = "Professional-Customer messaging")
public class CommunicationController {

    private final CommunicationRepository communicationRepository;
    private final ProfessionalRepository professionalRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;

    @GetMapping
    @Operation(summary = "Get messages between professional and a customer")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable Long professionalId,
            @RequestParam Long customerId) {
        List<Map<String, Object>> messages = communicationRepository
                .findByProfessionalIdAndCustomerIdOrderByCreatedAtDesc(professionalId, customerId)
                .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/threads")
    @Operation(summary = "Get all customer threads for a professional")
    public ResponseEntity<List<Map<String, Object>>> getThreads(@PathVariable Long professionalId) {
        // Get distinct customers this professional has communicated with
        List<Communication> all = communicationRepository.findAll().stream()
                .filter(c -> c.getProfessional().getId().equals(professionalId))
                .collect(Collectors.toList());

        Map<Long, Communication> latestByCustomer = new java.util.LinkedHashMap<>();
        for (Communication c : all) {
            Long cid = c.getCustomer().getId();
            if (!latestByCustomer.containsKey(cid) ||
                c.getCreatedAt().isAfter(latestByCustomer.get(cid).getCreatedAt())) {
                latestByCustomer.put(cid, c);
            }
        }

        List<Map<String, Object>> threads = latestByCustomer.values().stream().map(c -> {
            Map<String, Object> t = new java.util.HashMap<>();
            t.put("customerId", c.getCustomer().getId());
            t.put("customerName", c.getCustomer().getName());
            t.put("lastMessage", c.getMessage().length() > 60
                    ? c.getMessage().substring(0, 60) + "..." : c.getMessage());
            t.put("createdAt", c.getCreatedAt());
            long unread = communicationRepository
                    .findByProfessionalIdAndCustomerIdOrderByCreatedAtDesc(professionalId, c.getCustomer().getId())
                    .stream().filter(m -> !m.isRead()).count();
            t.put("unread", unread);
            return t;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(threads);
    }

    @PostMapping
    @Operation(summary = "Send a message to a customer")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long professionalId,
            @RequestBody Map<String, Object> body) {

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        Long customerId = body.get("customerId") != null
                ? Long.parseLong(body.get("customerId").toString()) : null;
        if (customerId == null) return ResponseEntity.badRequest().build();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        String typeStr = body.get("type") != null ? body.get("type").toString() : "GENERAL";
        CommunicationType type;
        try { type = CommunicationType.valueOf(typeStr); }
        catch (Exception e) { type = CommunicationType.GENERAL; }

        Communication comm = Communication.builder()
                .professional(professional)
                .customer(customer)
                .message((String) body.get("message"))
                .type(type)
                .isRead(false)
                .build();

        if (body.get("appointmentId") != null) {
            Long apptId = Long.parseLong(body.get("appointmentId").toString());
            appointmentRepository.findById(apptId).ifPresent(comm::setAppointment);
        }

        Communication saved = communicationRepository.save(comm);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(saved));
    }

    private Map<String, Object> toMap(Communication c) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", c.getId());
        m.put("professionalId", c.getProfessional().getId());
        m.put("customerId", c.getCustomer().getId());
        m.put("customerName", c.getCustomer().getName());
        m.put("message", c.getMessage());
        m.put("type", c.getType().name());
        m.put("isRead", c.isRead());
        m.put("createdAt", c.getCreatedAt());
        if (c.getAppointment() != null) m.put("appointmentId", c.getAppointment().getId());
        return m;
    }
}
