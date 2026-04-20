package org.example.common.mapper;

import org.example.common.dto.category.CategoryListDto;
import org.example.common.entity.ProductCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryListDto toListDto(ProductCategory category) {
        return new CategoryListDto(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
