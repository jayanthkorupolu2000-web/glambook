package com.salon.service.impl;

import com.salon.dto.request.ResourceAvailabilityRequest;
import com.salon.dto.request.ResourceRequest;
import com.salon.dto.response.ResourceAvailabilityResponse;
import com.salon.dto.response.ResourceResponse;
import com.salon.entity.Resource;
import com.salon.entity.ResourceAvailability;
import com.salon.entity.ResourceType;
import com.salon.entity.SalonOwner;
import com.salon.exception.InvalidOperationException;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.ResourceAvailabilityRepository;
import com.salon.repository.ResourceRepository;
import com.salon.repository.SalonOwnerRepository;
import com.salon.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityRepository availabilityRepository;
    private final SalonOwnerRepository salonOwnerRepository;

    @Override
    @Transactional
    public ResourceResponse addResource(ResourceRequest dto) {
        SalonOwner owner = salonOwnerRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + dto.getOwnerId()));

        Resource resource = Resource.builder()
                .owner(owner)
                .type(ResourceType.valueOf(dto.getType()))
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        return toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public ResourceResponse addAvailabilitySlot(Long resourceId, ResourceAvailabilityRequest dto) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new InvalidOperationException("End time must be after start time");
        }

        boolean exists = availabilityRepository
                .findByResourceIdAndAvailDateAndStartTime(resourceId, dto.getAvailDate(), dto.getStartTime())
                .isPresent();
        if (exists) {
            throw new InvalidOperationException("Duplicate slot: resource already has a slot at this date and time");
        }

        ResourceAvailability slot = ResourceAvailability.builder()
                .resource(resource)
                .availDate(dto.getAvailDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .build();

        availabilityRepository.save(slot);
        return toResponse(resourceRepository.findById(resourceId).get());
    }

    @Override
    public List<ResourceResponse> getResourcesByOwner(Long ownerId) {
        return resourceRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResourceResponse updateResourceAvailability(Long resourceId, boolean isAvailable) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
        resource.setAvailable(isAvailable);
        return toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public void markSlotBooked(Long slotId) {
        ResourceAvailability slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));
        if (slot.isBooked()) {
            throw new InvalidOperationException("This resource slot is already booked");
        }
        slot.setBooked(true);
        availabilityRepository.save(slot);
    }

    private ResourceResponse toResponse(Resource r) {
        ResourceResponse res = new ResourceResponse();
        res.setId(r.getId());
        res.setOwnerId(r.getOwner().getId());
        res.setType(r.getType().name());
        res.setName(r.getName());
        res.setDescription(r.getDescription());
        res.setAvailable(r.isAvailable());

        List<ResourceAvailabilityResponse> slots = availabilityRepository.findByResourceId(r.getId())
                .stream().map(s -> {
                    ResourceAvailabilityResponse sr = new ResourceAvailabilityResponse();
                    sr.setId(s.getId());
                    sr.setResourceId(r.getId());
                    sr.setAvailDate(s.getAvailDate());
                    sr.setStartTime(s.getStartTime());
                    sr.setEndTime(s.getEndTime());
                    sr.setBooked(s.isBooked());
                    return sr;
                }).collect(Collectors.toList());
        res.setAvailabilitySlots(slots);
        return res;
    }
}
