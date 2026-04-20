package org.example.data.repository;

import org.example.common.entity.UserCoupon;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Integer> {

    @EntityGraph(attributePaths = {"coupon"})
    @Query("select uc from UserCoupon uc where uc.user.id = :userId order by uc.createdAt desc")
    List<UserCoupon> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    @Query("select uc from UserCoupon uc where uc.user.id = :userId and uc.coupon.id = :couponId")
    Optional<UserCoupon> findByUserIdAndCouponId(@Param("userId") Integer userId, @Param("couponId") Integer couponId);

    boolean existsByCouponId(Integer couponId);

    long countByCouponId(Integer couponId);

    @Transactional
    @Modifying
    @Query("update UserCoupon uc set uc.active = false where uc.coupon.id = :couponId")
    void deactivateByCouponId(@Param("couponId") Integer couponId);

    @Transactional
    @Modifying
    @Query("update UserCoupon uc set uc.active = true where uc.coupon.id = :couponId")
    void reactivateByCouponId(@Param("couponId") Integer couponId);

    @Transactional
    @Modifying
    @Query("delete from UserCoupon uc where uc.coupon.id = :couponId")
    void deleteByCouponId(@Param("couponId") Integer couponId);

    @Transactional
    @Modifying
    @Query("delete from UserCoupon uc where uc.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}