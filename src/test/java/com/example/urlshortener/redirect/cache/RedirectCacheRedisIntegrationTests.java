package com.example.urlshortener.redirect.cache;

import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.analytics.service.LocalQueueClickEventPublisher;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RedirectCacheRedisIntegrationTests {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.redirect.cache.enabled", () -> "true");
        registry.add("app.redirect.cache.ttl", () -> "5m");
        registry.add("app.redirect.cache.jitter", () -> "0s");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired RedirectCacheService cacheService;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired ClickEventRepository clickEventRepository;
    @Autowired ShortUrlRepository shortUrlRepository;
    @Autowired UserRepository userRepository;
    @Autowired LocalQueueClickEventPublisher clickAnalyticsPublisher;

    @BeforeEach
    void cleanDatabase() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        refreshTokenRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void redirectShouldPopulateRedisAndDisableShouldInvalidateAfterCommit() throws Exception {
        String authorization = authorizationHeader("cache-owner@example.com");
        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/cache-target",
                                "customAlias", "cache123",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/r/cache123"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/cache-target"));
        clickAnalyticsPublisher.flushOnce();

        assertThat(redisTemplate.opsForValue().get(cacheService.key("cache123"))).isNotBlank();

        String id = shortUrlRepository.findByShortCode("cache123").orElseThrow().getId().toString();
        mockMvc.perform(post("/api/v1/urls/{id}/disable", id).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        assertThat(redisTemplate.opsForValue().get(cacheService.key("cache123"))).isNull();
        mockMvc.perform(get("/r/cache123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("disabled")));
    }

    private String authorizationHeader(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isCreated());
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
