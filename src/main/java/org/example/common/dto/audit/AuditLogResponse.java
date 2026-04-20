package org.example.common.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditLogResponse {
    private String actorName;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String targetType;
    private Integer targetId;
    private String details;
    private LocalDateTime createdAt;
}
