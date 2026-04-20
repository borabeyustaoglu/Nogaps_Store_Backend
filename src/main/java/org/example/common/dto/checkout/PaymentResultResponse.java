package org.example.common.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PaymentResultResponse {
    private Integer orderId;
    private String orderNumber;
    private String status;
    private String transactionId;
    private String message;
}