package com.erdsketch.workspace;

import com.erdsketch.auth.UserResponse;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(UUID workspaceId, UUID userId, UserResponse user, WorkspaceRole role, Instant joinedAt) {
    public static WorkspaceMemberResponse from(WorkspaceMember m) {
        return new WorkspaceMemberResponse(m.getWorkspace().getId(), m.getUser().getId(), UserResponse.from(m.getUser()), m.getRole(), m.getJoinedAt());
    }
}
