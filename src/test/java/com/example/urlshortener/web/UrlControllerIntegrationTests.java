package com.example.urlshortener.web;

import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.analytics.service.ClickAnalyticsPublisher;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlControllerIntegrationTests {

    private static final String OWNER_EMAIL = "placeholder@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ClickAnalyticsPublisher clickAnalyticsPublisher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createShouldValidateRequestAndReturnSafeDto() throws Exception {
        String authorization = authorizationHeader();

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/path",
                                "customAlias", "docs-123",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.shortCode").value("docs-123"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/path"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    void createShouldReturnProblemDetailsForValidationFailuresAndMalformedJson() throws Exception {
        String authorization = authorizationHeader();

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customAlias", "ok",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://example.com/problem/validation-failed"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail", containsString("originalUrl")))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(content().string(not(containsString("Exception"))));

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void createShouldRejectInvalidUrlAliasAndPastExpiration() throws Exception {
        String authorization = authorizationHeader();

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "ftp://example.com/file",
                                "customAlias", "ok",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com",
                                "customAlias", "bad.alias",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com",
                                "customAlias", "ok",
                                "expiresAt", LocalDateTime.now().minusDays(1).toString()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void managementEndpointsShouldSupportPaginationUpdateEnableDisableAndDelete() throws Exception {
        String authorization = authorizationHeader();
        UUID id = createUrl("manage1", "https://example.com/manage", LocalDateTime.now().plusDays(1), true).getId();

        mockMvc.perform(get("/api/v1/urls?page=0&size=10"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/urls?page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shortCode").value("manage1"));

        mockMvc.perform(get("/api/v1/urls?page=-1&size=0")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(patch("/api/v1/urls/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "expiresAt", LocalDateTime.now().plusDays(2).toString()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").exists());

        mockMvc.perform(post("/api/v1/urls/{id}/disable", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/v1/urls/{id}/enable", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(delete("/api/v1/urls/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/urls/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownershipShouldComeFromJwtSubjectAndBlockOtherUsers() throws Exception {
        String ownerAuthorization = authorizationHeader("owner-a@example.com");
        String otherAuthorization = authorizationHeader("owner-b@example.com");
        UUID injectedOwnerId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/owned",
                                "customAlias", "owned-a",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString(),
                                "ownerId", injectedOwnerId.toString(),
                                "userId", injectedOwnerId.toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("owned-a"))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());

        UserEntity owner = userRepository.findByEmail("owner-a@example.com").orElseThrow();
        ShortUrlEntity created = shortUrlRepository.findByShortCode("owned-a").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(created.getOwner().getId()).isEqualTo(owner.getId());
        org.assertj.core.api.Assertions.assertThat(created.getOwner().getId()).isNotEqualTo(injectedOwnerId);

        mockMvc.perform(get("/api/v1/urls/{id}", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/urls/{id}", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "expiresAt", LocalDateTime.now().plusDays(2).toString()
                        ))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/urls/{id}/disable", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/urls/{id}", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/api/v1/admin/urls")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
                .andExpect(status().isForbidden());
    }

    @Test
    void redirectShouldReturnLocationAndHandleFailureStates() throws Exception {
        ShortUrlEntity active = createUrl("go12345", "https://example.com/target", LocalDateTime.now().plusDays(1), true);

        mockMvc.perform(get("/r/{shortCode}", active.getShortCode()))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/target"))
                .andExpect(content().string(""));
        clickAnalyticsPublisher.flushOnce();
        assertClickCount(active.getId(), 1);
        org.assertj.core.api.Assertions.assertThat(clickEventRepository.findByUrl(active)).hasSize(1);

        mockMvc.perform(get("/r/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        createUrl("off1234", "https://example.com/off", LocalDateTime.now().plusDays(1), false);
        mockMvc.perform(get("/r/off1234"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("disabled")));

        createUrl("old1234", "https://example.com/old", LocalDateTime.now().minusDays(1), true);
        mockMvc.perform(get("/r/old1234"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("expired")));

        active.setDeleted(true);
        active.setEnabled(false);
        shortUrlRepository.saveAndFlush(active);
        mockMvc.perform(get("/r/{shortCode}", active.getShortCode()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminModerationShouldBlockUnblockAndInvalidateRedirects() throws Exception {
        String ownerAuthorization = authorizationHeader();
        String adminAuthorization = adminAuthorizationHeader("admin@example.com");
        ShortUrlEntity active = createUrl("blockme1", "https://example.com/blocked", LocalDateTime.now().plusDays(1), true);

        mockMvc.perform(post("/api/v1/admin/urls/{id}/block", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/urls/{id}/block", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/admin/urls/{id}/block", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/r/{shortCode}", active.getShortCode()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/urls/{id}/enable", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Blocked")));

        mockMvc.perform(post("/api/v1/admin/urls/{id}/unblock", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/r/{shortCode}", active.getShortCode()))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/blocked"));
    }

    @Test
    void openApiShouldExposeControllersAndHideInternalTypes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/urls']").exists())
                .andExpect(jsonPath("$.paths['/r/{shortCode}']").exists())
                .andExpect(jsonPath("$.components.schemas.CreateShortUrlRequest").exists())
                .andExpect(content().string(not(containsString("UserEntity"))))
                .andExpect(content().string(not(containsString("ShortUrlRepository"))));
    }

    private ShortUrlEntity createUrl(String code, String originalUrl, LocalDateTime expiresAt, boolean enabled) {
        UserEntity owner = userRepository.findByEmail(OWNER_EMAIL)
                .orElseGet(() -> userRepository.saveAndFlush(new UserEntity(UUID.randomUUID(), OWNER_EMAIL, "hash", UserRole.USER, UserStatus.ACTIVE)));
        ShortUrlEntity entity = new ShortUrlEntity(UUID.randomUUID(), code, originalUrl, code, owner, expiresAt);
        entity.setEnabled(enabled);
        return shortUrlRepository.saveAndFlush(entity);
    }

    private void assertClickCount(UUID id, long count) {
        AssertionError lastFailure = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                ShortUrlEntity entity = shortUrlRepository.findById(id).orElseThrow();
                org.assertj.core.api.Assertions.assertThat(entity.getClickCount()).isEqualTo(count);
                return;
            } catch (AssertionError failure) {
                lastFailure = failure;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw failure;
                }
            }
        }
        throw lastFailure;
    }

    private String authorizationHeader() throws Exception {
        return authorizationHeader(OWNER_EMAIL);
    }

    private String authorizationHeader(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "correct-horse-password"
                        ))))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "correct-horse-password"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return "Bearer " + json.get("accessToken").asText();
    }

    private String adminAuthorizationHeader(String email) throws Exception {
        userRepository.saveAndFlush(new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode("correct-horse-password"), UserRole.ADMIN, UserStatus.ACTIVE));
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "correct-horse-password"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return "Bearer " + json.get("accessToken").asText();
    }
}
