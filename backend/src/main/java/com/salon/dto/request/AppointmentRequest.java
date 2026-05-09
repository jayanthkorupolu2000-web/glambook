package com.salon.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @NotNull(message = "Please provide a valid customerId")
    private Long customerId;

    @NotNull(message = "Please provide a valid professionalId")
    private Long professionalId;

    @NotNull(message = "Please provide a valid serviceId")
    private Long serviceId;

    @NotNull(message = "Please provide a valid dateTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]", shape = JsonFormat.Shape.STRING)
    private LocalDateTime dateTime;
}
