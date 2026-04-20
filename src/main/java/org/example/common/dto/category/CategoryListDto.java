package org.example.common.dto.category;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CategoryListDto {
    private Integer id;
    private String name;
    private String description;
}
