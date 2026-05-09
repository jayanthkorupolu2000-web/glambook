package com.salon.repository;

import com.salon.entity.Order;
import com.salon.entity.OrderDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByCustomerIdAndDeliveryStatus(Long customerId, OrderDeliveryStatus status);
}
