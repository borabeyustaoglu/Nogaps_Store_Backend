package org.example.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.business.service.CartService;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.cart.CartItemRequest;
import org.example.common.dto.cart.CartItemResponse;
import org.example.common.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Only User")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    @PreAuthorize("hasAuthority('CART_ADD')")
    @PostMapping
    @Operation(summary = "Add To Cart", description = "Only User")
    public ResponseEntity<MessageResponse> addToCart(@Valid @RequestBody CartItemRequest request) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PreAuthorize("hasAuthority('CART_REMOVE')")
    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove From Cart", description = "Only User")
    public ResponseEntity<MessageResponse> removeFromCart(@PathVariable Integer productId) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartService.removeFromCart(userId, productId));
    }

    @PreAuthorize("hasAuthority('CART_LIST')")
    @GetMapping({"", "/list", "/all"})
    @Operation(summary = "List Cart", description = "Only User")
    public ResponseEntity<List<CartItemResponse>> listCart() {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartService.listCart(userId));
    }
}
