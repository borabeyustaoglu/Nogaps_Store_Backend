package org.example.data.repository;

import org.example.common.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    @EntityGraph(attributePaths = {"product"})
    @Query("select ci from CartItem ci where ci.user.id = :userId")
    List<CartItem> findByCartUserId(@Param("userId") Integer userId);

    @Query("select ci from CartItem ci where ci.user.id = :userId and ci.product.id = :productId")
    Optional<CartItem> findByCartUserIdAndProductId(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Transactional
    @Modifying
    @Query("delete from CartItem ci where ci.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);

    @Transactional
    @Modifying
    @Query("delete from CartItem ci where ci.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}