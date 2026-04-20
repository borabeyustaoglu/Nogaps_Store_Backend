package org.example.common.dto.coupon;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CouponAssignRequest {
    private List<Integer> userIds;
    private Boolean active;
    private Integer usageLimit;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Boolean unlimitedDuration;
}