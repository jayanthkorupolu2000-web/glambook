package com.salon.service;

import com.salon.dto.request.SalonPolicyRequest;
import com.salon.dto.response.SalonPolicyResponse;

import java.util.List;

public interface SalonPolicyService {
    SalonPolicyResponse publishPolicy(SalonPolicyRequest dto);
    List<SalonPolicyResponse> getPoliciesByOwner(Long ownerId);
    SalonPolicyResponse getLatestPolicyByOwner(Long ownerId);
    /** Returns all policies published by the salon owner of the given city */
    List<SalonPolicyResponse> getPoliciesByCity(String city);
}
