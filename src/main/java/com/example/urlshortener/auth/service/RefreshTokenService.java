package com.example.urlshortener.auth.service;

import com.example.urlshortener.auth.config.AuthProperties;
import com.example.urlshortener.auth.entity.RefreshTokenEntity;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.user.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties properties;
    private final AppMetrics metrics;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AuthProperties properties, AppMetrics metrics) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public IssuedRefreshToken issue(UserEntity user) {
        return issue(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public IssuedRefreshToken rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshTokenEntity current = refreshTokenRepository.findByTokenDigest(digest(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (current.isRevoked()) {
            current.markReuseDetected(now);
            refreshTokenRepository.saveAndFlush(current);
            refreshTokenRepository.revokeActiveFamily(current.getFamilyId(), now);
            metrics.auth("refresh_reuse", "detected");
            log.warn("Refresh token reuse detected for token family {}", current.getFamilyId());
            throw new BadRequestException("Invalid refresh token");
        }
        if (current.isExpired(now)) {
            current.revoke(now);
            throw new BadRequestException("Invalid refresh token");
        }

        IssuedRefreshToken replacement = issue(current.getUser(), current.getFamilyId());
        current.replaceWith(replacement.entity(), now);
        return replacement;
    }

    @Transactional
    public void revokeIfPresent(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenDigest(digest(rawToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> token.revoke(LocalDateTime.now()));
    }

    public String digest(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to digest refresh token", ex);
        }
    }

    private IssuedRefreshToken issue(UserEntity user, UUID familyId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        RefreshTokenEntity entity = new RefreshTokenEntity(
                UUID.randomUUID(),
                user,
                digest(rawToken),
                familyId,
                now,
                now.plus(properties.getRefreshTokenTtl())
        );
        return new IssuedRefreshToken(rawToken, refreshTokenRepository.save(entity));
    }

    public record IssuedRefreshToken(String rawToken, RefreshTokenEntity entity) {
    }
}
