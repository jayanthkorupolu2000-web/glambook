package com.salon.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ResourceResponse {
    private Long id;
    private Long ownerId;
    private String type;
    private String name;
    private String description;
    private boolean isAvailable;
    private List<ResourceAvailabilityResponse> availabilitySlots;
}
