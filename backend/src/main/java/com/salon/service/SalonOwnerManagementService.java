package com.salon.service;

import com.salon.dto.request.SalonOwnerManagementEditRequest;
import com.salon.dto.response.SalonOwnerManagementResponse;

/**
 * Service interface for Admin-level Salon Owner management operations.
 */
public interface SalonOwnerManagementService {

    /**
     * Updates ownerName, salonName, and phone for the given Salon Owner.
     * Email and city are never modified.
     *
     * @param id  the Salon Owner's primary key
     * @param dto the fields to update
     * @return the updated Salon Owner as a response DTO
     */
    SalonOwnerManagementResponse updateSalonOwner(Long id, SalonOwnerManagementEditRequest dto);
}
