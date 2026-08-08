package com.example.urlshortener.audit;

import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.repository.AuditRepository;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.idempotency.repository.IdempotencyRecordRepository;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import com.example.urlshortener.workspace.repository.WorkspaceRepository;
import com.example.urlshortener.workspace.service.WorkspaceContextResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.public-base-url=https://short.example/")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditTrailIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuditRepository auditRepository;
    @Autowired UserRepository userRepository;
    @Autowired ShortUrlRepository shortUrlRepository;
    @Autowired ClickEventRepository clickEventRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired IdempotencyRecordRepository idempotencyRecordRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMembershipRepository membershipRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        auditRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        membershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void urlMutationsCreateBoundedRedactedAuditEventsWithoutIdempotencyDuplicates() throws Exception {
        String token = registerAndLogin("audit-owner@example.com");
        UUID workspaceId = defaultWorkspaceId(token);
        String createBody = json(Map.of(
                "originalUrl", "https://example.com/create?secret=query",
                "customAlias", "audit-one",
                "expiresAt", LocalDateTime.now().plusDays(1).toString()
        ));

        JsonNode created = postUrl(token, workspaceId, "audit-create-key", "audit-correlation-1", createBody)
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-ID", "audit-correlation-1"))
                .andReturnJson();
        UUID urlId = UUID.fromString(created.get("id").asText());
        String etag = mockMvc.perform(get("/api/v1/urls/{id}", urlId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.ETAG);

        postUrl(token, workspaceId, "audit-create-key", "audit-correlation-2", createBody)
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString())
                        .header("Idempotency-Key", "audit-create-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "originalUrl", "https://example.com/different",
                                "customAlias", "audit-two",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/urls/{id}/destination", urlId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString())
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("originalUrl", "https://example.org/new?token=hidden"))))
                .andExpect(status().isPreconditionFailed());

        mockMvc.perform(patch("/api/v1/urls/{id}/destination", urlId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString())
                        .header(HttpHeaders.IF_MATCH, etag)
                        .header("X-Correlation-ID", "audit-destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("originalUrl", "https://example.org/new?token=hidden"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/urls/{id}", urlId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("expiresAt", LocalDateTime.now().plusDays(2).toString()))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/urls/{id}/disable", urlId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/urls/{id}/enable", urlId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/urls/{id}", urlId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString()))
                .andExpect(status().isNoContent());

        assertThat(auditRepository.countByAction(AuditAction.URL_CREATED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.URL_DESTINATION_CHANGED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.URL_EXPIRATION_CHANGED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.URL_DISABLED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.URL_ENABLED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.URL_DELETED)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/urls/{id}/audit?action=URL_DESTINATION_CHANGED", urlId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("URL_DESTINATION_CHANGED"))
                .andExpect(jsonPath("$.content[0].workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.content[0].correlationId").value("audit-destination"))
                .andExpect(jsonPath("$.content[0].safeMetadata.previousHost").value("example.com"))
                .andExpect(jsonPath("$.content[0].safeMetadata.newHost").value("example.org"))
                .andExpect(jsonPath("$.content[0].safeMetadata.previousUrlHash").exists())
                .andExpect(jsonPath("$.content[0].safeMetadata.newUrlHash").exists())
                .andExpect(content().string(not(containsString("token=hidden"))))
                .andExpect(content().string(not(containsString("secret=query"))));
    }

    @Test
    void workspaceAuditRequiresOwnerOrAdminAndCapturesMembershipChanges() throws Exception {
        String ownerToken = registerAndLogin("workspace-owner@example.com");
        String adminToken = registerAndLogin("workspace-admin@example.com");
        String viewerToken = registerAndLogin("workspace-viewer@example.com");
        String outsiderToken = registerAndLogin("workspace-outsider@example.com");

        UUID workspaceId = createWorkspace(ownerToken, "Audit Team");
        addMember(ownerToken, workspaceId, "workspace-admin@example.com", "ADMIN");
        addMember(ownerToken, workspaceId, "workspace-viewer@example.com", "VIEWER");
        UUID viewerId = userRepository.findByEmail("workspace-viewer@example.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/workspaces/{id}/members/{userId}", workspaceId, viewerId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "ANALYST"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/workspaces/{id}/members/{userId}", workspaceId, viewerId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNoContent());

        assertThat(auditRepository.countByAction(AuditAction.WORKSPACE_CREATED)).isGreaterThanOrEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.WORKSPACE_MEMBER_ADDED)).isEqualTo(2);
        assertThat(auditRepository.countByAction(AuditAction.WORKSPACE_MEMBER_ROLE_CHANGED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.WORKSPACE_MEMBER_REMOVED)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/workspaces/{id}/audit?action=WORKSPACE_MEMBER_ROLE_CHANGED", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].safeMetadata.previousRole").value("VIEWER"))
                .andExpect(jsonPath("$.content[0].safeMetadata.newRole").value("ANALYST"));

        mockMvc.perform(get("/api/v1/workspaces/{id}/audit", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/workspaces/{id}/audit", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, viewerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/workspaces/{id}/audit", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, outsiderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void platformAdminCanQueryModerationAuditThroughExplicitAdminEndpoint() throws Exception {
        String ownerToken = registerAndLogin("moderation-owner@example.com");
        String adminToken = createPlatformAdminAndLogin("moderation-admin@example.com");
        UUID workspaceId = defaultWorkspaceId(ownerToken);
        JsonNode created = postUrl(ownerToken, workspaceId, "moderation-key", "moderation-create", json(Map.of(
                        "originalUrl", "https://example.com/moderation",
                        "customAlias", "audit-block",
                        "expiresAt", LocalDateTime.now().plusDays(1).toString()
                )))
                .andExpect(status().isCreated())
                .andReturnJson();
        UUID urlId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(post("/api/v1/admin/urls/{id}/block", urlId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .header("X-Correlation-ID", "audit-block"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/admin/urls/{id}/unblock", urlId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/audit?action=URL_BLOCKED")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/audit?action=URL_BLOCKED")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("URL_BLOCKED"))
                .andExpect(jsonPath("$.content[0].workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.content[0].resourceId").value(urlId.toString()))
                .andExpect(jsonPath("$.content[0].correlationId").value("audit-block"));

        assertThat(auditRepository.countByAction(AuditAction.URL_BLOCKED)).isEqualTo(1);
        assertThat(auditRepository.countByAction(AuditAction.URL_UNBLOCKED)).isEqualTo(1);
    }

    private ResultJson postUrl(String token, UUID workspaceId, String idempotencyKey, String correlationId, String body) throws Exception {
        return new ResultJson(mockMvc.perform(post("/api/v1/urls")
                .header(HttpHeaders.AUTHORIZATION, token)
                .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString())
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Correlation-ID", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)));
    }

    private UUID defaultWorkspaceId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get(0).get("id").asText());
    }

    private UUID createWorkspace(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private void addMember(String ownerToken, UUID workspaceId, String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/{id}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "role", role))))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String createPlatformAdminAndLogin(String email) throws Exception {
        userRepository.saveAndFlush(new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode("correct-horse-password"), UserRole.ADMIN, UserStatus.ACTIVE));
        return login(email);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private final class ResultJson {
        private final org.springframework.test.web.servlet.ResultActions actions;

        private ResultJson(org.springframework.test.web.servlet.ResultActions actions) {
            this.actions = actions;
        }

        private ResultJson andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            actions.andExpect(matcher);
            return this;
        }

        private JsonNode andReturnJson() throws Exception {
            return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
        }
    }
}
