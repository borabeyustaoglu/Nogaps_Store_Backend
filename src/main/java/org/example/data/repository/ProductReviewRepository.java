package org.example.data.repository;

import org.example.common.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Integer> {
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Integer productId);

    Optional<ProductReview> findByIdAndProductId(Integer id, Integer productId);

    @Transactional
    @Modifying
    @Query("delete from ProductReview pr where pr.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);

    @Transactional
    @Modifying
    @Query("delete from ProductReview pr where pr.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}