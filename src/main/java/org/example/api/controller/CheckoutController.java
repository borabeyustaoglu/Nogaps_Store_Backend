package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.business.service.CheckoutService;
import org.example.common.dto.checkout.CheckoutPreviewRequest;
import org.example.common.dto.checkout.CheckoutPreviewResponse;
import org.example.common.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@Tag(name = "Checkout", description = "Only User")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final SecurityUtils securityUtils;

    @PreAuthorize("hasAuthority('CHECKOUT_PREVIEW')")
    @PostMapping("/preview")
    @Operation(summary = "Checkout Preview", description = "Preview order totals with coupon/installment")
    public ResponseEntity<CheckoutPreviewResponse> preview(@Valid @RequestBody(required = false) CheckoutPreviewRequest request) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(checkoutService.preview(userId, request));
    }
}