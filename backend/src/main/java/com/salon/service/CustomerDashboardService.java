package com.salon.service;

import com.salon.dto.response.CustomerDashboardResponse;

public interface CustomerDashboardService {
    CustomerDashboardResponse getDashboard(Long customerId);
}
