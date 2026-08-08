package com.example.urlshortener.apikey.entity;

import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
public class ApiKeyEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_api_keys_workspace"))
    private WorkspaceEntity workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_api_keys_created_by"))
    private UserEntity createdBy;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 32, unique = true)
    private String keyPrefix;

    @Column(name = "key_digest", nullable = false, length = 128, unique = true)
    private String keyDigest;

    @Column(nullable = false)
    private String scopes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public ApiKeyEntity() {
    }

    public ApiKeyEntity(UUID id, WorkspaceEntity workspace, UserEntity createdBy, String name,
                        String keyPrefix, String keyDigest, String scopes, LocalDateTime expiresAt) {
        this.id = id;
        this.workspace = workspace;
        this.createdBy = createdBy;
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.keyDigest = keyDigest;
        this.scopes = scopes;
        this.expiresAt = expiresAt;
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

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getKeyDigest() {
        return keyDigest;
    }

    public String getScopes() {
        return scopes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public long getVersion() {
        return version;
    }

    public void revoke(LocalDateTime revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
