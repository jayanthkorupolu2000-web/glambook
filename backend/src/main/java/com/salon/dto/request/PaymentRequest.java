package com.salon.dto.request;

import com.salon.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "Please provide a valid appointmentId")
    private Long appointmentId;

    @NotNull(message = "Please provide a valid amount")
    @DecimalMin(value = "0.01", message = "Please provide a valid amount")
    private BigDecimal amount;

    @NotNull(message = "Please provide a valid method")
    private PaymentMethod method;
}
