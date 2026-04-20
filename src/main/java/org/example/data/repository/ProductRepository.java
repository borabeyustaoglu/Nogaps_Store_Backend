package org.example.data.repository;

import org.example.common.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByOrderByIdAsc();

    @Query("select (count(p) > 0) from Product p where p.category.id = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Integer categoryId);
}
