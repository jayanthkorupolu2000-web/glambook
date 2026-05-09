package com.salon.repository;

import com.salon.entity.Gender;
import com.salon.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByGender(Gender gender);
    List<Service> findByProfessionalId(Long professionalId);
    List<Service> findByNameContainingIgnoreCase(String name);
    java.util.Optional<Service> findFirstByNameIgnoreCase(String name);
    List<Service> findByCategoryIgnoreCase(String category);

    /** Find distinct professional IDs who have at least one active service
     *  whose category contains any of the given keywords (case-insensitive). */
    @Query("SELECT DISTINCT s.professionalId FROM Service s " +
           "WHERE s.isActive = true AND s.professionalId IS NOT NULL AND " +
           "LOWER(s.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Long> findProfessionalIdsByCategoryKeyword(@Param("keyword") String keyword);
}
