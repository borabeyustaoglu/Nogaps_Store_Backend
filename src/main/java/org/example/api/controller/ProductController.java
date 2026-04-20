package org.example.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.business.service.ProductService;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.product.ProductListDto;
import org.example.common.dto.product.ProductRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Only Administrator")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping({"", "/list", "/all"})
    @Operation(summary = "List Products", description = "List all products")
    public ResponseEntity<List<ProductListDto>> list() {
        return ResponseEntity.ok(productService.listAll());
    }

    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @PostMapping
    @Operation(summary = "Create Product", description = "Only Administrator")
    public ResponseEntity<MessageResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }

    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @PutMapping("/{id}")
    @Operation(summary = "Update Product", description = "Only Administrator")
    public ResponseEntity<MessageResponse> update(@PathVariable Integer id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Product", description = "Only Administrator")
    public ResponseEntity<MessageResponse> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.delete(id));
    }
}
