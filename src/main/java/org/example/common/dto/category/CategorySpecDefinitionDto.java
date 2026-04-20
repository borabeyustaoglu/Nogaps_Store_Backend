package org.example.common.dto.category;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CategorySpecDefinitionDto {
    private String categoryName;
    private List<CategorySpecFieldDto> fields;
}
