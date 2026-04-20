package org.example.api.controller;

import lombok.RequiredArgsConstructor;
import org.example.business.service.FavoriteService;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.favorite.FavoriteProductResponse;
import org.example.common.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final SecurityUtils securityUtils;

    @PreAuthorize("isAuthenticated()")
    @GetMapping({"", "/list", "/all"})
    public ResponseEntity<List<FavoriteProductResponse>> list() {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(favoriteService.listFavorites(userId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{productId}")
    public ResponseEntity<MessageResponse> add(@PathVariable Integer productId) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(favoriteService.addFavorite(userId, productId));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{productId}")
    public ResponseEntity<MessageResponse> remove(@PathVariable Integer productId) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(favoriteService.removeFavorite(userId, productId));
    }
}