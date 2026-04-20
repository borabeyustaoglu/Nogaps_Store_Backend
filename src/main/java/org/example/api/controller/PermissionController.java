package org.example.api.controller;

import lombok.RequiredArgsConstructor;
import org.example.business.service.PermissionService;
import org.example.common.dto.permission.PermissionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "permissions (only manager and adminstrator)")
    public ResponseEntity<List<PermissionResponse>> list() {
        return ResponseEntity.ok(permissionService.listPermissions());
    }
}
