package org.example.common.dto.checkout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.common.entity.PaymentProvider;

@Getter
@Setter
public class OrderCreateRequest {

    private String couponCode;

    private Integer installmentCount;

    private PaymentProvider paymentProvider;

    @Valid
    @NotNull(message = "Teslimat adresi zorunludur.")
    private ShippingAddressRequest shippingAddress;
}