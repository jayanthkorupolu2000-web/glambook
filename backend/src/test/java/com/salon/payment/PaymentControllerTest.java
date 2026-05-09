package com.salon.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.controller.PaymentController;
import com.salon.dto.request.PaymentRequest;
import com.salon.dto.response.PaymentResponse;
import com.salon.entity.PaymentMethod;
import com.salon.exception.ResourceNotFoundException;
import com.salon.exception.ValidationException;
import com.salon.security.JwtUtil;
import com.salon.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest paymentRequest;
    private PaymentResponse paymentResponse;
    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer valid-jwt-token";
        
        paymentRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CASH)
                .build();

        paymentResponse = PaymentResponse.builder()
                .id(1L)
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method("CASH")
                .status("PAID")
                .paidAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processPayment_ValidRequest_ShouldReturn200() throws Exception {
        // Given
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(paymentService.processPayment(any(PaymentRequest.class), any(Long.class))).thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.appointmentId").value(1L))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.method").value("CASH"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void processPayment_InvalidAmount_ShouldReturn400() throws Exception {
        // Given
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("-10.00")) // Invalid negative amount
                .method(PaymentMethod.CASH)
                .build();

        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPayment_MissingFields_ShouldReturn400() throws Exception {
        // Given
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .appointmentId(null) // Missing required field
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CASH)
                .build();

        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPayment_AppointmentNotFound_ShouldReturn404() throws Exception {
        // Given
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(paymentService.processPayment(any(PaymentRequest.class), any(Long.class)))
                .thenThrow(new ResourceNotFoundException("Appointment not found"));

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void processPayment_AmountMismatch_ShouldReturn400() throws Exception {
        // Given
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(paymentService.processPayment(any(PaymentRequest.class), any(Long.class)))
                .thenThrow(new ValidationException("Payment amount must equal the service price"));

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPayment_WrongRole_ShouldReturn401() throws Exception {
        // Given
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("SALON_OWNER");

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void processPayment_Unauthenticated_ShouldReturn401() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void processPayment_WithCashMethod_ShouldSucceed() throws Exception {
        // Given
        PaymentRequest cashRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CASH)
                .build();

        PaymentResponse cashResponse = PaymentResponse.builder()
                .id(1L)
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method("CASH")
                .status("PAID")
                .build();

        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(paymentService.processPayment(any(PaymentRequest.class), any(Long.class))).thenReturn(cashResponse);

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cashRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("CASH"));
    }

    @Test
    void processPayment_WithCardMethod_ShouldSucceed() throws Exception {
        // Given
        PaymentRequest cardRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CARD)
                .build();

        PaymentResponse cardResponse = PaymentResponse.builder()
                .id(1L)
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method("CARD")
                .status("PAID")
                .build();

        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(paymentService.processPayment(any(PaymentRequest.class), any(Long.class))).thenReturn(cardResponse);

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("CARD"));
    }

    @Test
    void processPayment_WithUpiMethod_ShouldSucceed() throws Exception {
        // Given
        PaymentRequest upiRequest = PaymentRequest.builder()
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.UPI)
                .build();

        PaymentResponse upiResponse = PaymentResponse.builder()
                .id(1L)
                .appointmentId(1L)
                .amount(new BigDecimal("150.00"))
                .method("UPI")
                .status("PAID")
                .build();

        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-jwt-token")).thenReturn(1L);
        when(paymentService.processPayment(any(PaymentRequest.class), any(Long.class))).thenReturn(upiResponse);

        // When & Then
        mockMvc.perform(post("/api/payments")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(upiRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("UPI"));
    }

    @Test
    void getPaymentByAppointmentId_ValidId_ShouldReturn200() throws Exception {
        // Given
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(paymentService.getPaymentByAppointmentId(1L)).thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(get("/api/payments/1")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.appointmentId").value(1L))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.method").value("CASH"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void getPaymentByAppointmentId_PaymentNotFound_ShouldReturn404() throws Exception {
        // Given
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("CUSTOMER");
        when(paymentService.getPaymentByAppointmentId(1L))
                .thenThrow(new ResourceNotFoundException("Payment not found for appointment"));

        // When & Then
        mockMvc.perform(get("/api/payments/1")
                .header("Authorization", validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentByAppointmentId_WrongRole_ShouldReturn401() throws Exception {
        // Given
        when(jwtUtil.extractRole("valid-jwt-token")).thenReturn("SALON_OWNER");

        // When & Then
        mockMvc.perform(get("/api/payments/1")
                .header("Authorization", validToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPaymentByAppointmentId_Unauthenticated_ShouldReturn401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isUnauthorized());
    }
}