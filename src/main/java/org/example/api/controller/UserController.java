package org.example.api.controller;

import lombok.RequiredArgsConstructor;
import org.example.business.service.UserManagementService;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.user.UserListItemResponse;
import org.example.common.dto.user.UserUpdateRequest;
import org.example.common.security.AppUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class UserController {

    private final UserManagementService userManagementService;

    @GetMapping({"", "/list", "/all", "/getall"})
    public ResponseEntity<List<UserListItemResponse>> listUsers() {
        return ResponseEntity.ok(userManagementService.listUsers());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserListItemResponse> updateUser(
            @PathVariable Integer userId,
            @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return ResponseEntity.ok(userManagementService.updateUser(
                userId,
                request,
                actor.getUsername(),
                actor.getFullName(),
                actor.getRoleName()
        ));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(
            @PathVariable Integer userId,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return ResponseEntity.ok(userManagementService.deleteUser(
                userId,
                actor.getUsername(),
                actor.getFullName(),
                actor.getRoleName()
        ));
    }
}