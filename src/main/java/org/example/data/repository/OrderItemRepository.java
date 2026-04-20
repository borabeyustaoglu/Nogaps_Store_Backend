package org.example.data.repository;

import org.example.common.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findByOrderId(Integer orderId);

    void deleteByOrderId(Integer orderId);

    boolean existsByProductId(Integer productId);
}