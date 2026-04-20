package org.example.common.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.common.entity.CouponDiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MyCouponItemResponse {
    private Integer userCouponId;
    private Integer couponId;
    private String code;
    private CouponDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Boolean active;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer remainingUsage;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Boolean unlimitedDuration;
}