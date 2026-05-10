package com.salon.service;

import com.salon.dto.request.ComplaintRequest;
import com.salon.dto.request.MediationRequest;
import com.salon.dto.response.ComplaintResponse;

import java.util.List;

public interface ComplaintService {
    ComplaintResponse createComplaint(ComplaintRequest dto);
    List<ComplaintResponse> getAllComplaints();
    List<ComplaintResponse> getComplaintsByStatus(String status);
    List<ComplaintResponse> getComplaintsByCustomer(Long customerId);
    ComplaintResponse forwardComplaint(Long complaintId);
    ComplaintResponse mediateComplaint(Long complaintId, MediationRequest dto);
}
