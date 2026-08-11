package com.example.urlshortener.outbox;

import com.example.urlshortener.outbox.domain.DomainEvent;
import com.example.urlshortener.outbox.domain.DomainEventPublisher;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.public-base-url=https://short.example/",
        "app.outbox.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OutboxRollbackIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void cleanBefore() {
        clean();
    }

    @AfterEach
    void clean() {
        jdbcTemplate.update("delete from outbox_events");
        jdbcTemplate.update("delete from idempotency_keys");
        jdbcTemplate.update("delete from audit_events");
        jdbcTemplate.update("delete from api_keys");
        jdbcTemplate.update("delete from click_events");
        jdbcTemplate.update("delete from short_urls");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from workspace_memberships");
        jdbcTemplate.update("delete from workspaces");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void outboxFailureRollsBackRequiredUrlMutationAndAudit() throws Exception {
        doThrow(new IllegalStateException("outbox unavailable")).when(domainEventPublisher).publish(any(DomainEvent.class));
        String auth = registerAndLogin("owner@example.com");
        long setupAuditEvents = count("audit_events");

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/rollback",
                                "customAlias", "rollback1",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isInternalServerError());

        assertThat(count("short_urls")).isZero();
        assertThat(count("audit_events")).isEqualTo(setupAuditEvents);
        assertThat(countUrlCreatedAuditEvents()).isZero();
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }

    private long countUrlCreatedAuditEvents() {
        return jdbcTemplate.queryForObject("select count(*) from audit_events where action = 'URL_CREATED'", Long.class);
    }
}
