package com.salon.service;

import com.salon.dto.response.OwnerReportResponse;

public interface OwnerReportService {
    OwnerReportResponse generateReport(Long ownerId);
}
