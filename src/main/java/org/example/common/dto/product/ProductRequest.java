package org.example.common.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Urun adi bos olamaz.")
    private String name;

    private String description;

    @NotNull(message = "Urun fiyati zorunludur.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Urun fiyati 0'dan buyuk olmali.")
    private BigDecimal price;

    @NotNull(message = "Kategori secimi zorunludur.")
    private Integer categoryId;

    @NotNull(message = "Stok miktari zorunludur.")
    @Min(value = 0, message = "Stok miktari 0 veya daha buyuk olmali.")
    private Integer stockQuantity;

    private Map<String, String> specs;
}
