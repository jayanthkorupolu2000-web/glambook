package com.salon.service;

import com.salon.dto.request.ResourceAvailabilityRequest;
import com.salon.dto.request.ResourceRequest;
import com.salon.dto.response.ResourceResponse;

import java.util.List;

public interface ResourceService {
    ResourceResponse addResource(ResourceRequest dto);
    ResourceResponse addAvailabilitySlot(Long resourceId, ResourceAvailabilityRequest dto);
    List<ResourceResponse> getResourcesByOwner(Long ownerId);
    ResourceResponse updateResourceAvailability(Long resourceId, boolean isAvailable);
    void markSlotBooked(Long slotId);
}
