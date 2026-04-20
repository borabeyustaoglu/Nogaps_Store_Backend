package org.example.data.repository;

import org.example.common.entity.ProductFavorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Integer> {
    @EntityGraph(attributePaths = {"product", "product.category"})
    @Query("select pf from ProductFavorite pf where pf.user.id = :userId order by pf.id desc")
    List<ProductFavorite> findByUserId(@Param("userId") Integer userId);

    @Query("select pf from ProductFavorite pf where pf.user.id = :userId and pf.product.id = :productId")
    Optional<ProductFavorite> findByUserIdAndProductId(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Transactional
    @Modifying
    @Query("delete from ProductFavorite pf where pf.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);

    @Transactional
    @Modifying
    @Query("delete from ProductFavorite pf where pf.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}