package com.example.urlshortener.auth.entity;

import com.example.urlshortener.user.entity.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_family_id", columnList = "family_id"),
        @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token_digest", columnNames = "token_digest"))
public class RefreshTokenEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token_digest", nullable = false, length = 64)
    private String tokenDigest;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_token_id")
    private RefreshTokenEntity replacedByToken;

    @Column(name = "reuse_detected_at")
    private LocalDateTime reuseDetectedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected RefreshTokenEntity() {
    }

    public RefreshTokenEntity(UUID id, UserEntity user, String tokenDigest, UUID familyId, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.user = user;
        this.tokenDigest = tokenDigest;
        this.familyId = familyId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.createdAt = issuedAt;
    }

    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getTokenDigest() { return tokenDigest; }
    public UUID getFamilyId() { return familyId; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public RefreshTokenEntity getReplacedByToken() { return replacedByToken; }
    public LocalDateTime getReuseDetectedAt() { return reuseDetectedAt; }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(LocalDateTime now) {
        this.revokedAt = now;
    }

    public void replaceWith(RefreshTokenEntity replacement, LocalDateTime now) {
        this.replacedByToken = replacement;
        this.revokedAt = now;
    }

    public void markReuseDetected(LocalDateTime now) {
        this.reuseDetectedAt = now;
    }
}
