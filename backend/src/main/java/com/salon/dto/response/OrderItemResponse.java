package com.salon.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String brand;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
