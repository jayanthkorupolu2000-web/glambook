package com.salon.appointment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.controller.AppointmentController;
import com.salon.dto.request.AppointmentRequest;
import com.salon.dto.request.UpdateAppointmentStatusRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.entity.AppointmentStatus;
import com.salon.exception.ConflictException;
import com.salon.security.JwtUtil;
import com.salon.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AppointmentService appointmentService;
    @MockBean private JwtUtil jwtUtil;
    @Autowired private ObjectMapper objectMapper;

    private AppointmentRequest appointmentRequest;
    private AppointmentResponse appointmentResponse;
    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer valid-jwt-token";
        appointmentRequest = AppointmentRequest.builder()
                .customerId(1L).professionalId(1L).serviceId(1L)
                .dateTime(LocalDateTime.now().plusDays(1)).build();

        appointmentResponse = new AppointmentResponse();
        appointmentResponse.setId(1L);
        appointmentResponse.setCustomerId(1L);
        appointmentResponse.setCustomerName("Test Customer");
        appointmentResponse.setProfessionalId(1L);
        appointmentResponse.setProfessionalName("Test Professional");
        appointmentResponse.setServiceId(1L);
        appointmentResponse.setServiceName("Haircut");
        appointmentResponse.setScheduledAt(appointmentRequest.getDateTime());
        appointmentResponse.setStatus("PENDING");
    }

    @Test
    void createAppointment_ValidRequest_ShouldReturn201() throws Exception {
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(appointmentService.createAppointment(any())).thenReturn(appointmentResponse);

        mockMvc.perform(post("/api/appointments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerName").value("Test Customer"))
                .andExpect(jsonPath("$.professionalName").value("Test Professional"));
    }

    @Test
    void createAppointment_TimeSlotConflict_ShouldReturn409() throws Exception {
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(appointmentService.createAppointment(any())).thenThrow(new ConflictException("Time slot unavailable"));

        mockMvc.perform(post("/api/appointments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAppointmentsByCustomer_ValidRequest_ShouldReturn200() throws Exception {
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(appointmentService.getAppointmentsByCustomer(1L)).thenReturn(Arrays.asList(appointmentResponse));

        mockMvc.perform(get("/api/appointments")
                .header("Authorization", validToken)
                .param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].customerName").value("Test Customer"));
    }

    @Test
    void updateAppointmentStatus_ValidRequest_ShouldReturn200() throws Exception {
        UpdateAppointmentStatusRequest request = UpdateAppointmentStatusRequest.builder()
                .status(AppointmentStatus.CONFIRMED).build();
        AppointmentResponse updated = new AppointmentResponse();
        updated.setId(1L);
        updated.setStatus("CONFIRMED");

        when(appointmentService.updateAppointmentStatus(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/appointments/1/status")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void cancelAppointment_ValidRequest_ShouldReturn200() throws Exception {
        AppointmentResponse cancelled = new AppointmentResponse();
        cancelled.setId(1L);
        cancelled.setStatus("CANCELLED");
        when(appointmentService.cancelAppointment(1L)).thenReturn(cancelled);

        mockMvc.perform(patch("/api/appointments/1/cancel")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
