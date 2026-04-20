package org.example.data.repository;

import org.example.common.entity.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppRoleRepository extends JpaRepository<AppRole, Integer> {
    Optional<AppRole> findByName(String name);
}
