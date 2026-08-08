package com.example.urlshortener.workspace.entity;

import com.example.urlshortener.user.entity.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_memberships")
@IdClass(WorkspaceMembershipId.class)
public class WorkspaceMembershipEntity {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workspace_memberships_workspace"))
    private WorkspaceEntity workspace;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workspace_memberships_user"))
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceRole role;

    @Column(name = "default_membership", nullable = false)
    private boolean defaultMembership;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WorkspaceMembershipEntity() {
    }

    public WorkspaceMembershipEntity(WorkspaceEntity workspace, UserEntity user, WorkspaceRole role, boolean defaultMembership) {
        this.workspace = workspace;
        this.user = user;
        this.role = role;
        this.defaultMembership = defaultMembership;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public WorkspaceEntity getWorkspace() {
        return workspace;
    }

    public UserEntity getUser() {
        return user;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public void setRole(WorkspaceRole role) {
        this.role = role;
    }

    public boolean isDefaultMembership() {
        return defaultMembership;
    }
}
