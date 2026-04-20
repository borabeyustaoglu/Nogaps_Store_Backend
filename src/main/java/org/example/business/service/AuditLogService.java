package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.audit.AuditLogResponse;
import org.example.common.entity.AuditLog;
import org.example.common.security.SecurityUtils;
import org.example.data.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public void log(String action, String targetType, Integer targetId, String details) {
        AuditLog log = new AuditLog();
        log.setActorName(securityUtils.getCurrentUserFullNameOrSystem());
        log.setActorUsername(securityUtils.getCurrentUsernameOrSystem());
        log.setActorRole(securityUtils.getCurrentRoleOrSystem());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    @Transactional
    public void logAs(String actorUsername, String actorName, String actorRole,
                      String action, String targetType, Integer targetId, String details) {
        AuditLog log = new AuditLog();
        log.setActorUsername(actorUsername);
        log.setActorName(actorName);
        log.setActorRole(actorRole);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listLatest() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(log -> new AuditLogResponse(
                        log.getActorName(),
                        log.getActorUsername(),
                        log.getActorRole(),
                        log.getAction(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
