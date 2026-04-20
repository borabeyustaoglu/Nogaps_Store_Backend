package org.example.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.business.service.ProductReviewService;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.review.ProductReviewCreateRequest;
import org.example.common.dto.review.ProductReviewResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping
    public ResponseEntity<List<ProductReviewResponse>> list(@PathVariable Integer productId) {
        return ResponseEntity.ok(productReviewService.listByProductId(productId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ProductReviewResponse> create(
            @PathVariable Integer productId,
            @Valid @RequestBody ProductReviewCreateRequest request
    ) {
        return ResponseEntity.ok(productReviewService.create(productId, request));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Integer productId,
            @PathVariable Integer reviewId
    ) {
        return ResponseEntity.ok(productReviewService.delete(productId, reviewId));
    }
}
