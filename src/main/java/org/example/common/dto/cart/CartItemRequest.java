package org.example.common.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequest {

    @NotNull(message = "Urun secimi zorunludur.")
    private Integer productId;

    @NotNull(message = "Adet bilgisi zorunludur.")
    @Min(value = 1, message = "Adet en az 1 olmali.")
    private Integer quantity;
}
