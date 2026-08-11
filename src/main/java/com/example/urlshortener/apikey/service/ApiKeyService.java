package com.example.urlshortener.apikey.service;

import com.example.urlshortener.apikey.config.ApiKeyProperties;
import com.example.urlshortener.apikey.dto.ApiKeyDtos.ApiKeyCreatedResponse;
import com.example.urlshortener.apikey.dto.ApiKeyDtos.ApiKeyResponse;
import com.example.urlshortener.apikey.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.example.urlshortener.apikey.entity.ApiKeyEntity;
import com.example.urlshortener.apikey.entity.ApiKeyScope;
import com.example.urlshortener.apikey.repository.ApiKeyRepository;
import com.example.urlshortener.apikey.security.ApiKeyPrincipal;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.service.AuditService;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.user.service.OwnerIdentity;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.service.WorkspaceAuthorizationService;
import com.example.urlshortener.workspace.service.WorkspaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {
    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String KEY_PREFIX = "usk_live";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyProperties properties;
    private final WorkspaceAuthorizationService authorizationService;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RateLimiterService rateLimiter;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         ApiKeyProperties properties,
                         WorkspaceAuthorizationService authorizationService,
                         CurrentOwnerProvider currentOwnerProvider,
                         UserRepository userRepository,
                         AuditService auditService,
                         RateLimiterService rateLimiter,
                         Clock clock) {
        this.apiKeyRepository = apiKeyRepository;
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.currentOwnerProvider = currentOwnerProvider;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Transactional
    public ApiKeyCreatedResponse create(UUID workspaceId, CreateApiKeyRequest request) {
        WorkspaceContext workspace = authorizationService.requireMemberManager(workspaceId);
        OwnerIdentity owner = currentOwnerProvider.getCurrentOwner();
        UserEntity creator = userRepository.getReferenceById(owner.userId());
        Set<ApiKeyScope> scopes = parseScopes(request.getScopes());
        LocalDateTime expiresAt = resolveExpiration(request.getExpiresAt());
        GeneratedKey generated = generateUniqueRawKey();
        ApiKeyEntity entity = apiKeyRepository.save(new ApiKeyEntity(
                UUID.randomUUID(),
                workspace.workspace(),
                creator,
                request.getName().trim(),
                generated.prefix(),
                hmac(generated.rawKey()),
                serializeScopes(scopes),
                expiresAt
        ));
        auditService.recordUser(
                AuditAction.API_KEY_CREATED,
                workspaceId,
                owner.userId(),
                AuditResourceType.API_KEY,
                entity.getId(),
                auditService.apiKeyMetadata(entity.getId(), entity.getKeyPrefix(), entity.getName(), toValues(scopes), entity.getExpiresAt())
        );
        return new ApiKeyCreatedResponse(entity.getId(), entity.getName(), entity.getKeyPrefix(), generated.rawKey(), toValues(scopes), entity.getCreatedAt(), entity.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list(UUID workspaceId) {
        authorizationService.requireMemberManager(workspaceId);
        return apiKeyRepository.findByWorkspaceId(workspaceId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApiKeyResponse revoke(UUID workspaceId, UUID id) {
        WorkspaceContext workspace = authorizationService.requireMemberManager(workspaceId);
        OwnerIdentity owner = currentOwnerProvider.getCurrentOwner();
        ApiKeyEntity entity = apiKeyRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));
        boolean wasActive = entity.getRevokedAt() == null;
        entity.revoke(LocalDateTime.now(clock));
        ApiKeyResponse response = toResponse(apiKeyRepository.save(entity));
        if (wasActive) {
            auditService.recordUser(
                    AuditAction.API_KEY_REVOKED,
                    workspace.workspaceId(),
                    owner.userId(),
                    AuditResourceType.API_KEY,
                    entity.getId(),
                    auditService.apiKeyMetadata(entity.getId(), entity.getKeyPrefix(), entity.getName(), parseScopes(entity.getScopes()), entity.getExpiresAt())
            );
        }
        return response;
    }

    @Transactional
    public ApiKeyPrincipal authenticate(String rawKey) {
        ParsedKey parsed = parse(rawKey);
        ApiKeyEntity entity = apiKeyRepository.findByKeyPrefix(parsed.prefix())
                .orElseThrow(() -> new BadCredentialsException("Invalid API key"));
        byte[] expected = hex(entity.getKeyDigest());
        byte[] actual = hex(hmac(rawKey));
        if (!MessageDigest.isEqual(expected, actual) || entity.getRevokedAt() != null || isExpired(entity)) {
            throw new BadCredentialsException("Invalid API key");
        }
        WorkspaceEntity workspace = entity.getWorkspace();
        if (workspace == null) {
            throw new BadCredentialsException("Invalid API key");
        }
        rateLimiter.enforce("api-key", entity.getId().toString());
        updateLastUsed(entity.getId());
        return new ApiKeyPrincipal(entity.getId(), workspace.getId(), parseScopes(entity.getScopes()));
    }

    private void updateLastUsed(UUID id) {
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            apiKeyRepository.updateLastUsedIfOlder(id, now, now.minus(properties.getLastUsedUpdateInterval()));
        } catch (RuntimeException ex) {
            log.warn("API key last-used update failed for key id {}", id);
        }
    }

    private boolean isExpired(ApiKeyEntity entity) {
        return entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now(clock));
    }

    private LocalDateTime resolveExpiration(LocalDateTime requested) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime max = now.plus(properties.getMaxExpiry());
        LocalDateTime expiresAt = requested == null ? now.plus(properties.getDefaultExpiry()) : requested;
        if (expiresAt.isBefore(now)) {
            throw new BadRequestException("API key expiration must be in the future");
        }
        if (expiresAt.isAfter(max)) {
            throw new BadRequestException("API key expiration exceeds the configured maximum");
        }
        return expiresAt;
    }

    private GeneratedKey generateUniqueRawKey() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String prefix = randomPrefix();
            if (apiKeyRepository.findByKeyPrefix(prefix).isPresent()) {
                continue;
            }
            String rawKey = KEY_PREFIX + "_" + prefix + "_" + randomToken(32);
            return new GeneratedKey(prefix, rawKey);
        }
        throw new IllegalStateException("Unable to generate unique API key prefix");
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        secureRandom.nextBytes(value);
        return ENCODER.encodeToString(value);
    }

    private String randomPrefix() {
        byte[] value = new byte[6];
        secureRandom.nextBytes(value);
        return HexFormat.of().formatHex(value);
    }

    private ParsedKey parse(String rawKey) {
        if (rawKey == null || rawKey.isBlank() || rawKey.length() > properties.getMaxHeaderLength()) {
            throw new BadCredentialsException("Invalid API key");
        }
        String[] parts = rawKey.split("_", 4);
        if (parts.length != 4 || !"usk".equals(parts[0]) || !"live".equals(parts[1]) || parts[2].isBlank() || parts[3].isBlank()) {
            throw new BadCredentialsException("Invalid API key");
        }
        if (!parts[2].matches("^[A-Za-z0-9_-]{8,16}$") || !parts[3].matches("^[A-Za-z0-9_-]{32,128}$")) {
            throw new BadCredentialsException("Invalid API key");
        }
        return new ParsedKey(parts[2]);
    }

    private String hmac(String rawKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getHashPepper().getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate API key digest", ex);
        }
    }

    private byte[] hex(String value) {
        return HexFormat.of().parseHex(value);
    }

    private Set<ApiKeyScope> parseScopes(Set<String> values) {
        if (values == null || values.isEmpty()) {
            throw new BadRequestException("At least one API key scope is required");
        }
        return values.stream().map(ApiKeyScope::fromValue).collect(Collectors.toCollection(() -> EnumSet.noneOf(ApiKeyScope.class)));
    }

    private Set<ApiKeyScope> parseScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scopes.split(","))
                .map(ApiKeyScope::fromValue)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ApiKeyScope.class)));
    }

    private String serializeScopes(Set<ApiKeyScope> scopes) {
        return scopes.stream().map(ApiKeyScope::value).sorted().collect(Collectors.joining(","));
    }

    private Set<String> toValues(Set<ApiKeyScope> scopes) {
        return scopes.stream().map(ApiKeyScope::value).collect(Collectors.toCollection(TreeSet::new));
    }

    private ApiKeyResponse toResponse(ApiKeyEntity entity) {
        return new ApiKeyResponse(
                entity.getId(),
                entity.getName(),
                entity.getKeyPrefix(),
                parseScopes(entity.getScopes()).stream().map(ApiKeyScope::value).collect(Collectors.toCollection(TreeSet::new)),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getLastUsedAt()
        );
    }

    private record GeneratedKey(String prefix, String rawKey) {
    }

    private record ParsedKey(String prefix) {
    }
}
