package org.example.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ProductListDto {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer categoryId;
    private Integer stockQuantity;
    private String categoryName;
    private Map<String, String> specs;
}
