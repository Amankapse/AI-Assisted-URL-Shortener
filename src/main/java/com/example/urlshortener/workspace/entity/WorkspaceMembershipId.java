package com.example.urlshortener.workspace.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class WorkspaceMembershipId implements Serializable {
    private UUID workspace;
    private UUID user;

    public WorkspaceMembershipId() {
    }

    public WorkspaceMembershipId(UUID workspace, UUID user) {
        this.workspace = workspace;
        this.user = user;
    }

    public UUID getWorkspace() {
        return workspace;
    }

    public void setWorkspace(UUID workspace) {
        this.workspace = workspace;
    }

    public UUID getUser() {
        return user;
    }

    public void setUser(UUID user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkspaceMembershipId that)) return false;
        return Objects.equals(workspace, that.workspace) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, user);
    }
}
