package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long appointmentId;
    private BigDecimal amount;
    private String method;
    private String paymentType;
    private String status;
    private String transactionId;
    private String receiptUrl;
    private LocalDateTime paidAt;
}
