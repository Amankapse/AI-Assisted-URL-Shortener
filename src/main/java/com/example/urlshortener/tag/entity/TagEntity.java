package com.example.urlshortener.tag.entity;

import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tags")
public class TagEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tags_workspace"))
    private WorkspaceEntity workspace;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 50)
    private String normalizedName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TagEntity() {
    }

    public TagEntity(UUID id, WorkspaceEntity workspace, String name, String normalizedName) {
        this.id = id;
        this.workspace = workspace;
        this.name = name;
        this.normalizedName = normalizedName;
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public WorkspaceEntity getWorkspace() {
        return workspace;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
