package com.salon.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private Long customerId;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String deliveryStatus;
    private String deliveryAddress;
    private LocalDate estimatedDelivery;
    private LocalDateTime createdAt;
}
