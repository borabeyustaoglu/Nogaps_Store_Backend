package org.example.data.repository;

import org.example.common.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Integer> {

    Optional<ShippingAddress> findByOrderId(Integer orderId);
}