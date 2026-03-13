package com.erdsketch.workspace;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class WorkspaceMemberId implements Serializable {
    private UUID workspace;
    private UUID user;

    public WorkspaceMemberId() {}
    public WorkspaceMemberId(UUID workspace, UUID user) { this.workspace = workspace; this.user = user; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkspaceMemberId that)) return false;
        return Objects.equals(workspace, that.workspace) && Objects.equals(user, that.user);
    }
    @Override public int hashCode() { return Objects.hash(workspace, user); }
}
