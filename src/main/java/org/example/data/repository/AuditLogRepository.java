package org.example.data.repository;

import org.example.common.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
