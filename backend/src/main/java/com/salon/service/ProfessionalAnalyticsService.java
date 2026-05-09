package com.salon.service;

import com.salon.dto.response.ProfessionalAnalyticsResponse;

public interface ProfessionalAnalyticsService {
    ProfessionalAnalyticsResponse generateAnalytics(Long professionalId);
}
