package org.example.data.repository;

import org.example.common.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

    boolean existsByUserId(Integer userId);

    @EntityGraph(attributePaths = {"items", "shippingAddress"})
    Optional<Order> findDetailedById(Integer id);

    @EntityGraph(attributePaths = {"items", "shippingAddress"})
    @Query("select o from AppOrder o where o.id = :orderId and o.user.id = :userId")
    Optional<Order> findDetailedByIdAndUserId(@Param("orderId") Integer orderId, @Param("userId") Integer userId);

    @Query("select o from AppOrder o where o.id = :orderId and o.user.id = :userId")
    Optional<Order> findByIdAndUserId(@Param("orderId") Integer orderId, @Param("userId") Integer userId);
}