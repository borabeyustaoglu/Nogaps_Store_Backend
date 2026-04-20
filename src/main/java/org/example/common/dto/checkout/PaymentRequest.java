package org.example.common.dto.checkout;

import lombok.Getter;
import lombok.Setter;
import org.example.common.entity.PaymentProvider;

@Getter
@Setter
public class PaymentRequest {
    private PaymentProvider paymentProvider;
}