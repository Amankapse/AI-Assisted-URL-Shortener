package com.example.urlshortener.apikey;

import com.example.urlshortener.apikey.repository.ApiKeyRepository;
import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.repository.AuditRepository;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.common.ratelimit.RateLimitExceededException;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.idempotency.repository.IdempotencyRecordRepository;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.entity.WorkspaceRole;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import com.example.urlshortener.workspace.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.example.urlshortener.testsupport.WorkspaceTestSupport.defaultWorkspace;
import static com.example.urlshortener.testsupport.WorkspaceTestSupport.member;
import static com.example.urlshortener.testsupport.WorkspaceTestSupport.workspace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.public-base-url=https://short.example/",
        "app.rate-limit.policies.api-key.limit=1000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiKeyIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private RateLimiterService rateLimiter;

    @BeforeEach
    void cleanDatabase() {
        idempotencyRecordRepository.deleteAll();
        auditRepository.deleteAll();
        apiKeyRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        membershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanAfterTest() {
        cleanDatabase();
    }

    @Test
    void ownerAndAdminCanCreateKeysButLowerRolesCannot() throws Exception {
        String ownerAuth = authorizationHeader("owner@example.com", UserRole.USER);
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        WorkspaceEntity ownerWorkspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);

        createApiKey(ownerAuth, ownerWorkspace.getId(), Set.of("links:read"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawKey").value(containsString("usk_live_")))
                .andExpect(jsonPath("$.prefix").exists());

        UserEntity admin = user("workspace-admin@example.com");
        member(ownerWorkspace, admin, WorkspaceRole.ADMIN, membershipRepository);
        createApiKey(login("workspace-admin@example.com"), ownerWorkspace.getId(), Set.of("links:write"))
                .andExpect(status().isCreated());

        for (WorkspaceRole role : Set.of(WorkspaceRole.EDITOR, WorkspaceRole.ANALYST, WorkspaceRole.VIEWER)) {
            UserEntity user = user(role.name().toLowerCase() + "@example.com");
            WorkspaceEntity shared = workspace(user, role, workspaceRepository, membershipRepository);
            createApiKey(login(user.getEmail()), shared.getId(), Set.of("links:read"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void rawKeyIsReturnedOnceAndOnlyDigestIsStored() throws Exception {
        String ownerAuth = authorizationHeader("owner@example.com", UserRole.USER);
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);

        JsonNode created = objectMapper.readTree(createApiKey(ownerAuth, workspace.getId(), Set.of("links:read", "links:write"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String rawKey = created.get("rawKey").asText();
        assertThat(apiKeyRepository.findAll()).singleElement().satisfies(entity -> {
            assertThat(entity.getKeyDigest()).doesNotContain(rawKey);
            assertThat(entity.getKeyDigest()).hasSize(64);
            assertThat(entity.getKeyPrefix()).isEqualTo(created.get("prefix").asText());
        });

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/api-keys", workspace.getId())
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(rawKey))))
                .andExpect(jsonPath("$[0].rawKey").doesNotExist());
    }

    @Test
    void apiKeyCanUseScopedUrlApisAndCannotUseHumanOrAdminApis() throws Exception {
        String ownerAuth = authorizationHeader("owner@example.com", UserRole.USER);
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        String rawKey = rawKey(ownerAuth, workspace.getId(), Set.of("links:read", "links:write", "analytics:read"));

        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/v1/urls")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "machine-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/machine",
                                "customAlias", "machine1",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        UUID urlId = UUID.fromString(created.get("id").asText());
        mockMvc.perform(get("/api/v1/urls/{id}", urlId).header("X-API-Key", rawKey))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/urls/{id}/analytics", urlId).header("X-API-Key", rawKey))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", workspace.getId()).header("X-API-Key", rawKey))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/urls/{id}/block", urlId).header("X-API-Key", rawKey))
                .andExpect(status().isForbidden());

        UUID apiKeyId = apiKeyRepository.findAll().get(0).getId();
        assertThat(auditRepository.findAll()).anySatisfy(event -> {
            assertThat(event.getAction()).isEqualTo(AuditAction.URL_CREATED);
            assertThat(event.getActorType()).isEqualTo(AuditActorType.API_KEY);
            assertThat(event.getActorId()).isEqualTo(apiKeyId);
        });
    }

    @Test
    void apiKeyScopeAndWorkspaceIsolationAreEnforced() throws Exception {
        String ownerAuth = authorizationHeader("owner@example.com", UserRole.USER);
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        WorkspaceEntity workspaceA = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        WorkspaceEntity workspaceB = workspace(owner, WorkspaceRole.OWNER, workspaceRepository, membershipRepository);
        String readOnlyKey = rawKey(ownerAuth, workspaceA.getId(), Set.of("links:read"));

        mockMvc.perform(post("/api/v1/urls")
                        .header("X-API-Key", readOnlyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/denied",
                                "customAlias", "denied1",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/urls")
                        .header("X-API-Key", readOnlyKey)
                        .header("X-Workspace-ID", workspaceB.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRevokedExpiredMalformedAndDualCredentialRequestsAreRejectedGenerically() throws Exception {
        String ownerAuth = authorizationHeader("owner@example.com", UserRole.USER);
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        String rawKey = rawKey(ownerAuth, workspace.getId(), Set.of("links:read"));

        mockMvc.perform(get("/api/v1/urls").header("X-API-Key", "invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication is required."));
        mockMvc.perform(get("/api/v1/urls").header("X-API-Key", rawKey).header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isUnauthorized());

        UUID keyId = apiKeyRepository.findAll().get(0).getId();
        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/api-keys/{id}/revoke", workspace.getId(), keyId)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/urls").header("X-API-Key", rawKey))
                .andExpect(status().isUnauthorized());

        String expiredKey = rawKey(ownerAuth, workspace.getId(), Set.of("links:read"), LocalDateTime.now().plusDays(1));
        String expiredPrefix = expiredKey.split("_", 4)[2];
        assertThat(jdbcTemplate.update("update api_keys set expires_at = timestamp '2000-01-01 00:00:00' where key_prefix = ?", expiredPrefix)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/urls").header("X-API-Key", expiredKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiKeyRateLimitRejectionReturnsProblemDetails() throws Exception {
        String ownerAuth = authorizationHeader("owner@example.com", UserRole.USER);
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        String rawKey = rawKey(ownerAuth, workspace.getId(), Set.of("links:read"));

        doThrow(new RateLimitExceededException("api-key", 9))
                .when(rateLimiter).enforce(eq("api-key"), anyString());

        mockMvc.perform(get("/api/v1/urls")
                        .header("X-API-Key", rawKey)
                        .header("X-Correlation-ID", "machine-rate-limit-test"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "9"))
                .andExpect(jsonPath("$.type").value("https://example.com/problem/rate-limit-exceeded"))
                .andExpect(jsonPath("$.errorCode").value("rate_limit_exceeded"))
                .andExpect(jsonPath("$.correlationId").value("machine-rate-limit-test"));
    }

    @Test
    void apiKeyIdempotencyDoesNotCollideWithHumanUser() throws Exception {
        String ownerAuth = authorizationHeader("owner@example.com", UserRole.USER);
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        String rawKey = rawKey(ownerAuth, workspace.getId(), Set.of("links:write"));
        String body = objectMapper.writeValueAsString(Map.of(
                "originalUrl", "https://example.com/idempotent",
                "expiresAt", LocalDateTime.now().plusDays(1).toString()
        ));

        mockMvc.perform(post("/api/v1/urls")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "same-client-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .header("Idempotency-Key", "same-client-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/human",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isCreated());

        assertThat(idempotencyRecordRepository.findAll()).hasSize(2);
    }

    @Test
    void redirectEndpointIgnoresMalformedApiKeyHeader() throws Exception {
        UserEntity owner = user("owner@example.com");
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        shortUrlRepository.saveAndFlush(new ShortUrlEntity(UUID.randomUUID(), "public1", "https://example.com/public", null, owner, workspace, LocalDateTime.now().plusDays(1)));

        mockMvc.perform(get("/r/public1").header("X-API-Key", "malformed"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/public"));
    }

    private org.springframework.test.web.servlet.ResultActions createApiKey(String authorization, UUID workspaceId, Set<String> scopes) throws Exception {
        return createApiKey(authorization, workspaceId, scopes, LocalDateTime.now().plusDays(30));
    }

    private org.springframework.test.web.servlet.ResultActions createApiKey(String authorization, UUID workspaceId, Set<String> scopes, LocalDateTime expiresAt) throws Exception {
        return mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/api-keys", workspaceId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "integration",
                        "scopes", scopes,
                        "expiresAt", expiresAt.toString()
                ))));
    }

    private String rawKey(String authorization, UUID workspaceId, Set<String> scopes) throws Exception {
        return rawKey(authorization, workspaceId, scopes, LocalDateTime.now().plusDays(30));
    }

    private String rawKey(String authorization, UUID workspaceId, Set<String> scopes, LocalDateTime expiresAt) throws Exception {
        JsonNode created = objectMapper.readTree(createApiKey(authorization, workspaceId, scopes, expiresAt)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        return created.get("rawKey").asText();
    }

    private String authorizationHeader(String email, UserRole role) throws Exception {
        if (role == UserRole.ADMIN) {
            userRepository.saveAndFlush(new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode("correct-horse-password"), UserRole.ADMIN, UserStatus.ACTIVE));
            return login(email);
        }
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private UserEntity user(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.saveAndFlush(new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode("correct-horse-password"), UserRole.USER, UserStatus.ACTIVE)));
    }
}
