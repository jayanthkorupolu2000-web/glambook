package com.salon.service;

import com.salon.dto.request.PolicyRequest;
import com.salon.dto.response.PolicyResponse;

import java.util.List;

public interface PolicyService {
    PolicyResponse publishPolicy(Long adminId, PolicyRequest dto);
    PolicyResponse getLatestPolicy();
    List<PolicyResponse> getAllPolicies();
}
