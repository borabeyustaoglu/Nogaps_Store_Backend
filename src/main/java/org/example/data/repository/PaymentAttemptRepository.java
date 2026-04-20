package org.example.data.repository;

import org.example.common.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Integer> {

    List<PaymentAttempt> findByOrderIdOrderByAttemptedAtDesc(Integer orderId);

    Optional<PaymentAttempt> findTopByOrderIdOrderByAttemptedAtDesc(Integer orderId);
}