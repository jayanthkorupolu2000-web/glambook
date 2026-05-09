package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CancellationStatsResponse {
    private long total;
    private long sameDay;
    private long suspendedCustomers;
}
