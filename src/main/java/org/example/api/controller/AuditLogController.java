package org.example.api.controller;

import lombok.RequiredArgsConstructor;
import org.example.business.service.AuditLogService;
import org.example.common.dto.audit.AuditLogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    @Operation(summary = "logs (only manager and adminstrator)")
    public ResponseEntity<List<AuditLogResponse>> listLatest() {
        return ResponseEntity.ok(auditLogService.listLatest());
    }
}
