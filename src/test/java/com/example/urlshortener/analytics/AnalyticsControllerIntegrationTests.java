package com.example.urlshortener.analytics;

import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.analytics.service.ClickAnalyticsEvent;
import com.example.urlshortener.analytics.service.ClickAnalyticsWriter;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ShortUrlRepository shortUrlRepository;
    @Autowired ClickEventRepository clickEventRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ClickAnalyticsWriter writer;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void ownerAnalyticsShouldBeScopedAndGroupedDaily() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com");
        String otherToken = registerAndLogin("other@example.com");
        UserEntity owner = userRepository.findByEmail("owner@example.com").orElseThrow();
        ShortUrlEntity url = shortUrlRepository.saveAndFlush(new ShortUrlEntity(UUID.randomUUID(), "abc1234", "https://example.com/private?token=secret", null, owner, LocalDateTime.now().plusDays(1)));
        writer.persistBatch(List.of(
                new ClickAnalyticsEvent(UUID.randomUUID(), url.getId(), LocalDateTime.of(2026, 8, 7, 10, 0), "hash1", "desktop", "ref.example", "corr-1"),
                new ClickAnalyticsEvent(UUID.randomUUID(), url.getId(), LocalDateTime.of(2026, 8, 7, 11, 0), "hash2", "mobile", "ref.example", "corr-2")
        ));

        mockMvc.perform(get("/api/v1/urls/{id}/analytics", url.getId()).header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlId").value(url.getId().toString()))
                .andExpect(jsonPath("$.totalRedirects").value(2))
                .andExpect(jsonPath("$.state").value("active"))
                .andExpect(content().string(not(containsString("token=secret"))));

        mockMvc.perform(get("/api/v1/urls/{id}/analytics/daily", url.getId()).header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].day").value("2026-08-07"))
                .andExpect(jsonPath("$.days[0].redirects").value(2));

        mockMvc.perform(get("/api/v1/urls/{id}/analytics", url.getId()).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminAnalyticsShouldRequireAdminAndAvoidSensitiveLinkDetails() throws Exception {
        String userToken = registerAndLogin("user@example.com");
        String adminToken = createAdminAndLogin("admin@example.com");
        UserEntity owner = userRepository.findByEmail("user@example.com").orElseThrow();
        ShortUrlEntity url = shortUrlRepository.saveAndFlush(new ShortUrlEntity(UUID.randomUUID(), "top1234", "https://example.com/private?token=secret", null, owner, LocalDateTime.now().plusDays(1)));
        writer.persistBatch(List.of(new ClickAnalyticsEvent(UUID.randomUUID(), url.getId(), LocalDateTime.now(), "hash1", "desktop", "ref.example", "corr-1")));

        mockMvc.perform(get("/api/v1/admin/analytics/overview").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/analytics/overview").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalRedirects").value(1));

        mockMvc.perform(get("/api/v1/admin/analytics/top-links?limit=10").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortCode").value("top1234"))
                .andExpect(jsonPath("$[0].redirects").value(1))
                .andExpect(content().string(not(containsString("token=secret"))))
                .andExpect(content().string(not(containsString("user@example.com"))));

        mockMvc.perform(get("/api/v1/urls").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isForbidden());
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String createAdminAndLogin(String email) throws Exception {
        userRepository.saveAndFlush(new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode("correct-horse-password"), UserRole.ADMIN, UserStatus.ACTIVE));
        return login(email);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return "Bearer " + json.get("accessToken").asText();
    }
}
