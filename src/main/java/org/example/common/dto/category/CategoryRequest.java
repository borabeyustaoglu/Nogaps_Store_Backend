package org.example.common.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Kategori adi bos olamaz.")
    private String name;

    private String description;
}
