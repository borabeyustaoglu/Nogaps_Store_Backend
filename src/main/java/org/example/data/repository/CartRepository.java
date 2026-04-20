package org.example.data.repository;

import org.example.common.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUserId(Integer userId);

    @Transactional
    @Modifying
    @Query("delete from Cart c where c.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}