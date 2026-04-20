package org.example.common.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.common.dto.product.ProductListDto;
import org.example.common.entity.Product;
import org.example.common.entity.ProductCategory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ObjectMapper objectMapper;

    public ProductListDto toListDto(Product product) {
        ProductCategory category = product.getCategory();
        Integer categoryId = category != null ? category.getId() : null;
        String categoryName = category != null ? category.getName() : null;
        Map<String, String> specs = parseSpecs(product.getSpecsJson());

        return new ProductListDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                categoryId,
                product.getStockQuantity(),
                categoryName,
                specs
        );
    }

    private Map<String, String> parseSpecs(String rawSpecsJson) {
        if (rawSpecsJson == null || rawSpecsJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(rawSpecsJson, new TypeReference<>() {
            });
            return parsed != null ? parsed : new LinkedHashMap<>();
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }
}
