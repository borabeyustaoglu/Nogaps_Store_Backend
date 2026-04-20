package org.example.common.dto.coupon;

import lombok.Getter;
import lombok.Setter;
import org.example.common.entity.CouponDiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CouponCreateRequest {
    private String code;
    private CouponDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Boolean active;
    private Integer usageLimit;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Boolean unlimitedDuration;

    private List<Integer> userIds;
    private Integer perUserUsageLimit;
}