package com.erdsketch.workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(UUID id, String name, String slug, UUID ownerId, Instant createdAt, Instant updatedAt) {
    public static WorkspaceResponse from(Workspace ws) {
        return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug(), ws.getOwner().getId(), ws.getCreatedAt(), ws.getUpdatedAt());
    }
}
