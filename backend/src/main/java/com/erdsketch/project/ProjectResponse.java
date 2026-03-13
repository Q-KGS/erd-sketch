package com.erdsketch.project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(UUID id, UUID workspaceId, String name, String description, DbType targetDbType, UUID createdBy, Instant createdAt, Instant updatedAt) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getWorkspace().getId(), p.getName(), p.getDescription(), p.getTargetDbType(), p.getCreatedBy().getId(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
