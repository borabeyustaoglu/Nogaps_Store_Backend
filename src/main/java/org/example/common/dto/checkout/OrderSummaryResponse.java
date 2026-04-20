package org.example.common.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class OrderSummaryResponse {
    private Integer orderId;
    private String orderNumber;
    private String status;
    private Integer itemCount;
    private BigDecimal grandTotal;
    private LocalDateTime createdAt;
}