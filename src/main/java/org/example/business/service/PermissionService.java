package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.permission.PermissionResponse;
import org.example.data.repository.AppPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final AppPermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(permission -> new PermissionResponse(permission.getCode(), permission.getDescription()))
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .toList();
    }
}
