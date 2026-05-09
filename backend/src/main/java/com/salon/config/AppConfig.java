package com.salon.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {

    /**
     * Configures ModelMapper with STRICT matching strategy.
     *
     * Service-layer classes use modelMapper.map(entity, ResponseDto.class) for conversions.
     * For nested objects (e.g., Professional → ProfessionalResponse with embedded SalonOwnerResponse),
     * service classes manually set nested DTOs after the top-level map call, or use explicit
     * TypeMap configurations added below.
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }
}
