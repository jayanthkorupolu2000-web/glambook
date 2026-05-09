package com.salon.service;

import com.salon.dto.request.SalonOwnerEditRequest;
import com.salon.dto.response.SalonOwnerEditResponse;

/**
 * Service interface for editing Salon Owner details.
 */
public interface SalonOwnerEditService {

    /**
     * Updates the name and phone of a Salon Owner identified by id.
     *
     * @param id  the Salon Owner's ID
     * @param dto contains the new name and phone values
     * @return the updated Salon Owner as a response DTO
     */
    SalonOwnerEditResponse updateSalonOwner(Long id, SalonOwnerEditRequest dto);
}
