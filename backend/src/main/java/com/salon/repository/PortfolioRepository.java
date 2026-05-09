package com.salon.repository;

import com.salon.entity.Portfolio;
import com.salon.entity.PortfolioMediaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByProfessionalId(Long professionalId);
    List<Portfolio> findByProfessionalIdAndIsFeatured(Long professionalId, boolean isFeatured);
    List<Portfolio> findByProfessionalIdAndMediaType(Long professionalId, PortfolioMediaType mediaType);
    List<Portfolio> findByProfessionalIdAndServiceTag(Long professionalId, String serviceTag);
    long countByProfessionalId(Long professionalId);
}
