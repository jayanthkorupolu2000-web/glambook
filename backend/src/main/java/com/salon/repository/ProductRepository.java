package com.salon.repository;

import com.salon.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByIsActiveTrue();
    List<Product> findByCategoryAndIsActiveTrue(String category);
    List<Product> findByBrandContainingIgnoreCaseAndIsActiveTrue(String brand);

    @Query("SELECT p FROM Product p WHERE (p.name LIKE %:kw% OR p.description LIKE %:kw% OR p.brand LIKE %:kw%) AND p.isActive = true")
    List<Product> searchProducts(@Param("kw") String keyword);
}
