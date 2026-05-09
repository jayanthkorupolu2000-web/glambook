package com.salon.dto.request;

import com.salon.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAppointmentStatusRequest {

    @NotNull(message = "Please provide a valid status")
    private AppointmentStatus status;
}
