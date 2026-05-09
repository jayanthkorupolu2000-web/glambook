package com.salon.repository;

import com.salon.entity.ProductOrder;
import com.salon.entity.ProductOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {

    List<ProductOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

    @Query("SELECT po FROM ProductOrder po WHERE po.customer.id = :customerId AND po.status = :status")
    List<ProductOrder> findByCustomerIdAndStatus(
            @Param("customerId") Long customerId,
            @Param("status") ProductOrderStatus status);

    /** True if the customer has at least one DELIVERED order for this product */
    @Query("SELECT COUNT(po) > 0 FROM ProductOrder po WHERE po.customer.id = :customerId " +
           "AND po.product.id = :productId AND po.status = 'DELIVERED'")
    boolean existsDeliveredOrderByCustomerIdAndProductId(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId);
}
