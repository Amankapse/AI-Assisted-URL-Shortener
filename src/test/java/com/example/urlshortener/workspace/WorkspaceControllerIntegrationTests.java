package com.example.urlshortener.workspace;

import com.example.urlshortener.analytics.repository.ClickEventRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceControllerIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
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
        idempotencyRecordRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        membershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationCreatesDefaultWorkspaceAndWorkspaceRolesControlUrlAccess() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com");
        String editorToken = registerAndLogin("editor@example.com");
        String viewerToken = registerAndLogin("viewer@example.com");
        String outsiderToken = registerAndLogin("outsider@example.com");

        JsonNode ownerWorkspaces = getJson("/api/v1/workspaces", ownerToken);
        assertThat(ownerWorkspaces).hasSize(1);
        UUID defaultWorkspaceId = UUID.fromString(ownerWorkspaces.get(0).get("id").asText());
        assertThat(ownerWorkspaces.get(0).get("role").asText()).isEqualTo("OWNER");
        assertThat(ownerWorkspaces.get(0).get("defaultWorkspace").asBoolean()).isTrue();

        UUID sharedWorkspaceId = createWorkspace(ownerToken, "Shared Team");
        addMember(ownerToken, sharedWorkspaceId, "editor@example.com", "EDITOR");
        addMember(ownerToken, sharedWorkspaceId, "viewer@example.com", "VIEWER");

        JsonNode created = createUrl(editorToken, sharedWorkspaceId, "team-docs", "idem-team-docs");
        UUID urlId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/urls/{id}", urlId)
                        .header(HttpHeaders.AUTHORIZATION, outsiderToken)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, sharedWorkspaceId.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/urls/{id}/analytics", urlId)
                        .header(HttpHeaders.AUTHORIZATION, viewerToken)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, sharedWorkspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlId").value(urlId.toString()))
                .andExpect(jsonPath("$.totalRedirects").value(0));

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, viewerToken)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, sharedWorkspaceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "originalUrl", "https://example.com/viewer",
                                "customAlias", "viewer-denied",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isForbidden());

        JsonNode defaultUrl = createUrl(ownerToken, defaultWorkspaceId, "default-docs", "same-key");
        JsonNode sharedUrl = createUrl(ownerToken, sharedWorkspaceId, "shared-docs", "same-key");
        assertThat(defaultUrl.get("shortCode").asText()).isEqualTo("default-docs");
        assertThat(sharedUrl.get("shortCode").asText()).isEqualTo("shared-docs");
    }

    @Test
    void platformAdminIsNotImplicitWorkspaceAdmin() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com");
        UUID workspaceId = createWorkspace(ownerToken, "Product");
        String adminToken = createPlatformAdminAndLogin("admin@example.com");

        mockMvc.perform(get("/api/v1/workspaces/{id}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCannotRemoveLastOwnerAndInvalidWorkspaceHeaderDoesNotFallback() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com");
        UUID workspaceId = UUID.fromString(getJson("/api/v1/workspaces", ownerToken).get(0).get("id").asText());
        UUID ownerId = userRepository.findByEmail("owner@example.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/workspaces/{id}/members/{userId}", workspaceId, ownerId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "VIEWER"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    private UUID createWorkspace(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.role").value("OWNER"))
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role));
    }

    private JsonNode createUrl(String token, UUID workspaceId, String alias, String idempotencyKey) throws Exception {
        String body = mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(WorkspaceContextResolver.WORKSPACE_HEADER, workspaceId.toString())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "originalUrl", "https://example.com/" + alias,
                                "customAlias", alias,
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value(alias))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode getJson(String path, String token) throws Exception {
        String body = mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
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
}
