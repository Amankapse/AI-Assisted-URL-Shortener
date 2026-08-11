package com.example.urlshortener.organization;

import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.entity.WorkspaceRole;
import com.example.urlshortener.workspace.repository.WorkspaceMembershipRepository;
import com.example.urlshortener.workspace.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.example.urlshortener.testsupport.WorkspaceTestSupport.defaultWorkspace;
import static com.example.urlshortener.testsupport.WorkspaceTestSupport.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.public-base-url=https://short.example/",
        "app.rate-limit.policies.api-key.limit=1000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationSearchIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMembershipRepository membershipRepository;

    @BeforeEach
    void cleanDatabase() {
        deleteRows();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {
        deleteRows();
    }

    private void deleteRows() {
        jdbcTemplate.update("delete from outbox_events");
        jdbcTemplate.update("delete from audit_events");
        jdbcTemplate.update("delete from idempotency_keys");
        jdbcTemplate.update("delete from click_events");
        jdbcTemplate.update("delete from url_tags");
        jdbcTemplate.update("delete from short_urls");
        jdbcTemplate.update("delete from tags");
        jdbcTemplate.update("delete from campaigns");
        jdbcTemplate.update("delete from api_keys");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from workspace_memberships");
        jdbcTemplate.update("delete from workspaces");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void campaignLifecycleHonorsRolesAndDeleteDetachesUrls() throws Exception {
        String ownerAuth = registerAndLogin("owner@example.com");
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        UserEntity viewer = registerUser("viewer@example.com");
        member(workspace, viewer, WorkspaceRole.VIEWER, membershipRepository);
        String viewerAuth = login("viewer@example.com");

        JsonNode campaign = createCampaign(ownerAuth, workspace.getId(), "Fall Launch", "email campaign");
        UUID campaignId = UUID.fromString(campaign.get("id").asText());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/campaigns", workspace.getId()).header(HttpHeaders.AUTHORIZATION, viewerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].normalizedName").value("fall launch"));

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/campaigns", workspace.getId())
                        .header(HttpHeaders.AUTHORIZATION, viewerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Denied"))))
                .andExpect(status().isForbidden());

        JsonNode url = createUrl(ownerAuth, workspace.getId(), "campaign1", "https://example.com/campaign", campaignId, "launch", "paid");
        UUID urlId = UUID.fromString(url.get("id").asText());

        mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/campaigns/{campaignId}", workspace.getId(), campaignId)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/urls/{id}", urlId)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .header("X-Workspace-ID", workspace.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign").doesNotExist())
                .andExpect(jsonPath("$.shortCode").value("campaign1"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_events where action = 'CAMPAIGN_DELETED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void createSearchAndMetadataUpdatesAreWorkspaceScopedAndOptimistic() throws Exception {
        String ownerAuth = registerAndLogin("search-owner@example.com");
        UserEntity owner = userRepository.findByEmail("search-owner@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        String otherAuth = registerAndLogin("other@example.com");
        UserEntity other = userRepository.findByEmail("other@example.com").orElseThrow();
        WorkspaceEntity otherWorkspace = defaultWorkspace(other, workspaceRepository, membershipRepository);
        UUID campaignId = UUID.fromString(createCampaign(ownerAuth, workspace.getId(), "Retail Ads", null).get("id").asText());
        UUID otherCampaignId = UUID.fromString(createCampaign(otherAuth, otherWorkspace.getId(), "Other Ads", null).get("id").asText());

        JsonNode created = createUrl(ownerAuth, workspace.getId(), "retail1", "https://shop.example.com/path?secret=hidden", campaignId, "Promo", "PAID", "paid");
        UUID urlId = UUID.fromString(created.get("id").asText());
        assertThat(created.get("tags").get(0).asText()).isEqualTo("paid");
        assertThat(created.get("tags").get(1).asText()).isEqualTo("promo");
        assertThat(created.get("campaign").get("normalizedName").asText()).isEqualTo("retail ads");

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .header("X-Workspace-ID", workspace.getId().toString())
                        .param("q", "shop.example.com")
                        .param("tag", "paid")
                        .param("campaignId", campaignId.toString())
                        .param("state", "ACTIVE")
                        .param("sort", "shortCode,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shortCode").value("retail1"))
                .andExpect(jsonPath("$.content[0].state").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .header("X-Workspace-ID", workspace.getId().toString())
                        .param("q", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .header("X-Workspace-ID", otherWorkspace.getId().toString())
                        .param("q", "retail1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        String etag = mockMvc.perform(get("/api/v1/urls/{id}", urlId)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .header("X-Workspace-ID", workspace.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(patch("/api/v1/urls/{id}/campaign", urlId)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .header("X-Workspace-ID", workspace.getId().toString())
                        .header(HttpHeaders.IF_MATCH, etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("campaignId", otherCampaignId))))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/urls/{id}/tags", urlId)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .header("X-Workspace-ID", workspace.getId().toString())
                        .header(HttpHeaders.IF_MATCH, etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tags", Set.of("evergreen", "paid")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("evergreen"))
                .andExpect(jsonPath("$.tags[1]").value("paid"));

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .header("X-Workspace-ID", workspace.getId().toString())
                        .param("sort", "originalUrl,asc"))
                .andExpect(status().isBadRequest());

        assertThat(jdbcTemplate.queryForObject("select destination_host from short_urls where id = ?", String.class, urlId)).isEqualTo("shop.example.com");
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_events where action in ('URL_CAMPAIGN_CHANGED','URL_TAGS_CHANGED')", Integer.class)).isEqualTo(1);
    }

    @Test
    void idempotencyFingerprintUsesNormalizedSortedTagSet() throws Exception {
        String auth = registerAndLogin("idem-tags@example.com");
        UserEntity owner = userRepository.findByEmail("idem-tags@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        UUID campaignId = UUID.fromString(createCampaign(auth, workspace.getId(), "Idem Campaign", null).get("id").asText());
        String expiresAt = LocalDateTime.now().plusDays(1).toString();

        String firstBody = objectMapper.writeValueAsString(Map.of(
                "originalUrl", "https://example.com/idem-tags",
                "customAlias", "idemtags",
                "expiresAt", expiresAt,
                "campaignId", campaignId.toString(),
                "tags", java.util.List.of("Beta", "alpha", "alpha")
        ));
        String secondBody = objectMapper.writeValueAsString(Map.of(
                "originalUrl", "https://example.com/idem-tags",
                "customAlias", "idemtags",
                "expiresAt", expiresAt,
                "campaignId", campaignId.toString(),
                "tags", java.util.List.of("alpha", "beta")
        ));
        JsonNode first = objectMapper.readTree(mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .header("X-Workspace-ID", workspace.getId().toString())
                        .header("Idempotency-Key", "stage8-idem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .header("X-Workspace-ID", workspace.getId().toString())
                        .header("Idempotency-Key", "stage8-idem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(first.get("id").asText()));

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .header("X-Workspace-ID", workspace.getId().toString())
                        .header("Idempotency-Key", "stage8-idem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/idem-tags",
                                "customAlias", "idemtags",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString(),
                                "campaignId", campaignId.toString(),
                                "tags", java.util.List.of("alpha", "gamma")
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void apiKeysCanReadMetadataAndWriteUrlMetadataButCannotManageCampaignLifecycle() throws Exception {
        String ownerAuth = registerAndLogin("machine-owner@example.com");
        UserEntity owner = userRepository.findByEmail("machine-owner@example.com").orElseThrow();
        WorkspaceEntity workspace = defaultWorkspace(owner, workspaceRepository, membershipRepository);
        UUID campaignId = UUID.fromString(createCampaign(ownerAuth, workspace.getId(), "Machine Campaign", null).get("id").asText());
        JsonNode url = createUrl(ownerAuth, workspace.getId(), "machine-meta", "https://example.com/machine-meta", null, "old");
        UUID urlId = UUID.fromString(url.get("id").asText());
        String apiKey = rawKey(ownerAuth, workspace.getId(), Set.of("links:read", "links:write"));

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/campaigns", workspace.getId()).header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(campaignId.toString()));
        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/tags", workspace.getId()).header("X-API-Key", apiKey))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/campaigns", workspace.getId())
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Denied"))))
                .andExpect(status().isForbidden());

        String etag = mockMvc.perform(get("/api/v1/urls/{id}", urlId).header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        mockMvc.perform(patch("/api/v1/urls/{id}/campaign", urlId)
                        .header("X-API-Key", apiKey)
                        .header(HttpHeaders.IF_MATCH, etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("campaignId", campaignId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.id").value(campaignId.toString()));
    }

    @Test
    void tagsAreValidatedAndIsolatedPerWorkspace() throws Exception {
        String firstAuth = registerAndLogin("first@example.com");
        String secondAuth = registerAndLogin("second@example.com");
        WorkspaceEntity first = defaultWorkspace(userRepository.findByEmail("first@example.com").orElseThrow(), workspaceRepository, membershipRepository);
        WorkspaceEntity second = defaultWorkspace(userRepository.findByEmail("second@example.com").orElseThrow(), workspaceRepository, membershipRepository);

        createUrl(firstAuth, first.getId(), "same-tag-a", "https://a.example.com", null, "shared");
        createUrl(secondAuth, second.getId(), "same-tag-b", "https://b.example.com", null, "shared");

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/tags", first.getId()).header(HttpHeaders.AUTHORIZATION, firstAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].normalizedName").value("shared"));

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, firstAuth)
                        .header("X-Workspace-ID", first.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/bad-tag",
                                "customAlias", "badtag",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString(),
                                "tags", java.util.List.of("bad tag")
                        ))))
                .andExpect(status().isBadRequest());

        assertThat(jdbcTemplate.queryForObject("select count(*) from tags where normalized_name = 'shared'", Integer.class)).isEqualTo(2);
    }

    private JsonNode createCampaign(String authorization, UUID workspaceId, String name, String description) throws Exception {
        Map<String, String> body = description == null ? Map.of("name", name) : Map.of("name", name, "description", description);
        String response = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/campaigns", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode createUrl(String authorization, UUID workspaceId, String alias, String originalUrl, UUID campaignId, String... tags) throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("originalUrl", originalUrl);
        body.put("customAlias", alias);
        body.put("expiresAt", LocalDateTime.now().plusDays(1).toString());
        if (campaignId != null) {
            body.put("campaignId", campaignId.toString());
        }
        if (tags != null && tags.length > 0) {
            body.put("tags", java.util.Arrays.asList(tags));
        }
        String response = mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .header("X-Workspace-ID", workspaceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String rawKey(String authorization, UUID workspaceId, Set<String> scopes) throws Exception {
        String response = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/api-keys", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "stage8",
                                "scopes", scopes,
                                "expiresAt", LocalDateTime.now().plusDays(30).toString()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("rawKey").asText();
    }

    private UserEntity registerUser(String email) throws Exception {
        registerAndLogin(email);
        return userRepository.findByEmail(email).orElseThrow();
    }

    private String registerAndLogin(String email) throws Exception {
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
}
