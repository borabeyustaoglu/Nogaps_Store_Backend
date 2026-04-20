package org.example.common.dto.category;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CategorySpecFieldDto {
    private String key;
    private String label;
    private List<String> options;
}
