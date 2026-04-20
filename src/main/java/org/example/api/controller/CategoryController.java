package org.example.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.business.service.CategoryService;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.category.CategoryListDto;
import org.example.common.dto.category.CategorySpecDefinitionDto;
import org.example.common.dto.category.CategoryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "Only Manager")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping({"", "/list", "/all"})
    @Operation(summary = "List Categories", description = "List all categories")
    public ResponseEntity<List<CategoryListDto>> list() {
        return ResponseEntity.ok(categoryService.listAll());
    }

    @GetMapping("/specs")
    @Operation(summary = "Category Specs", description = "Category based specification definitions")
    public ResponseEntity<List<CategorySpecDefinitionDto>> specs() {
        return ResponseEntity.ok(categoryService.listCategorySpecs());
    }

    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    @PostMapping
    @Operation(summary = "Create Category", description = "Only Manager")
    public ResponseEntity<MessageResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.create(request));
    }

    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    @PutMapping("/{id}")
    @Operation(summary = "Update Category", description = "Only Manager")
    public ResponseEntity<MessageResponse> update(@PathVariable Integer id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Category", description = "Only Manager")
    public ResponseEntity<MessageResponse> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.delete(id));
    }
}
