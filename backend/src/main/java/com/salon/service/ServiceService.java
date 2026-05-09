package com.salon.service;

import com.salon.dto.response.ServiceResponse;
import com.salon.entity.Service;
import com.salon.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final ModelMapper modelMapper;

    public Map<String, Map<String, List<ServiceResponse>>> getAllServicesGrouped() {
        return serviceRepository.findAll().stream()
                .filter(s -> s.getGender() != null && s.getCategory() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getGender().name(),
                        Collectors.groupingBy(
                                Service::getCategory,
                                Collectors.mapping(this::mapToResponse, Collectors.toList())
                        )
                ));
    }

    public List<ServiceResponse> getAllServicesList() {
        return serviceRepository.findAll().stream()
                .filter(s -> s.getName() != null)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ServiceResponse mapToResponse(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory())
                .gender(service.getGender() != null ? service.getGender().name() : null)
                .price(service.getPrice())
                .durationMins(service.getDurationMins())
                .build();
    }
}
