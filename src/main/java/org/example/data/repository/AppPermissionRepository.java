package org.example.data.repository;

import org.example.common.entity.AppPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppPermissionRepository extends JpaRepository<AppPermission, Integer> {
    Optional<AppPermission> findByCode(String code);
}
