package org.example.common.dto.checkout;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutPreviewRequest {
    private String couponCode;
    private Integer installmentCount;
}