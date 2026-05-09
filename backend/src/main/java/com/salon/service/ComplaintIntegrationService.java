package com.salon.service;

import com.salon.dto.request.MediationActionRequest;
import com.salon.dto.response.ComplaintResponse;

import java.util.List;

public interface ComplaintIntegrationService {
    List<ComplaintResponse> getForwardedComplaintsForOwner(Long ownerId);
    List<ComplaintResponse> getAllComplaintsForOwner(Long ownerId);
    ComplaintResponse logOwnerAction(Long ownerId, Long complaintId, MediationActionRequest dto);
}
