package com.example.urlshortener.outbox;

import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.outbox.config.OutboxProperties;
import com.example.urlshortener.outbox.domain.DomainEvent;
import com.example.urlshortener.outbox.domain.OutboxEventHandler;
import com.example.urlshortener.outbox.domain.OutboxEventRecord;
import com.example.urlshortener.outbox.domain.OutboxEventType;
import com.example.urlshortener.outbox.domain.OutboxFailureType;
import com.example.urlshortener.outbox.domain.OutboxHandlingException;
import com.example.urlshortener.outbox.domain.OutboxStatus;
import com.example.urlshortener.outbox.payload.UrlCacheInvalidationPayloadV1;
import com.example.urlshortener.outbox.service.OutboxDispatcher;
import com.example.urlshortener.outbox.service.OutboxEventStore;
import com.example.urlshortener.outbox.service.OutboxInstanceId;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.public-base-url=https://short.example/",
        "app.outbox.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OutboxIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OutboxEventStore store;
    @Autowired OutboxDispatcher dispatcher;
    @Autowired OutboxProperties properties;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired Clock clock;

    @BeforeEach
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
    void urlCreateCommitsBusinessAuditAndOutboxTogether() throws Exception {
        String auth = registerAndLogin("owner@example.com", "USER");

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "https://example.com/created",
                                "customAlias", "outboxcreate",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isCreated());

        assertThat(count("short_urls")).isEqualTo(1);
        assertThat(count("audit_events")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from outbox_events where event_type = 'URL_CREATED'", Long.class)).isEqualTo(1);
    }

    @Test
    void businessFailureDoesNotCreateOutboxEvent() throws Exception {
        String auth = registerAndLogin("owner@example.com", "USER");

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "originalUrl", "ftp://example.com/not-allowed",
                                "expiresAt", LocalDateTime.now().plusDays(1).toString()
                        ))))
                .andExpect(status().isBadRequest());

        assertThat(count("short_urls")).isZero();
        assertThat(count("outbox_events")).isZero();
    }

    @Test
    void dispatcherProcessesSuccessfulCacheInvalidationHandler() {
        publish(OutboxEventType.URL_CACHE_INVALIDATION_REQUIRED, new UrlCacheInvalidationPayloadV1(UUID.randomUUID(), "abc12345"));

        dispatcher.processOnceForTests();

        assertThat(statuses()).containsExactly(OutboxStatus.PROCESSED.name());
    }

    @Test
    void multipleClaimersDoNotClaimSameRows() {
        int previousBatchSize = properties.getBatchSize();
        properties.setBatchSize(2);
        try {
            for (int i = 0; i < 4; i++) {
                publish(OutboxEventType.URL_CREATED, Map.of("sequence", i));
            }

            List<OutboxEventRecord> first = store.claimBatch("instance-a");
            List<OutboxEventRecord> second = store.claimBatch("instance-b");

            assertThat(first).hasSize(2);
            assertThat(second).hasSize(2);
            assertThat(first).extracting(OutboxEventRecord::id)
                    .doesNotContainAnyElementsOf(second.stream().map(OutboxEventRecord::id).toList());
        } finally {
            properties.setBatchSize(previousBatchSize);
        }
    }

    @Test
    void claimTimeoutMakesProcessingRowsRecoverable() {
        UUID id = publish(OutboxEventType.URL_CREATED, Map.of("recover", true));
        store.claimBatch("instance-a");
        jdbcTemplate.update("update outbox_events set claimed_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now(clock).minus(properties.getClaimTimeout()).minusSeconds(1)),
                id);

        List<OutboxEventRecord> recovered = store.claimBatch("instance-b");

        assertThat(recovered).singleElement().satisfies(event -> {
            assertThat(event.id()).isEqualTo(id);
            assertThat(event.claimedBy()).isEqualTo("instance-b");
        });
    }

    @Test
    void transientFailureRetriesThenDeadAfterMaxAttempts() {
        OutboxProperties props = new OutboxProperties();
        props.setBatchSize(10);
        props.setMaxAttempts(1);
        props.setBaseBackoff(Duration.ofMillis(1));
        props.setMaxBackoff(Duration.ofMillis(5));
        AtomicInteger attempts = new AtomicInteger();
        OutboxDispatcher failingDispatcher = new OutboxDispatcher(
                store,
                List.of(failingHandler(attempts)),
                props,
                new OutboxInstanceId(),
                new AppMetrics(new SimpleMeterRegistry()),
                Clock.systemDefaultZone()
        );
        publish(OutboxEventType.URL_CREATED, Map.of("dead", true));

        failingDispatcher.processOnceForTests();

        assertThat(attempts).hasValue(1);
        assertThat(statuses()).containsExactly(OutboxStatus.DEAD.name());
        assertThat(jdbcTemplate.queryForObject("select last_error_code from outbox_events", String.class)).isEqualTo("temporary_failure");
    }

    @Test
    void adminOutboxEndpointIsAdminOnlyAndDoesNotReturnPayload() throws Exception {
        publish(OutboxEventType.URL_CREATED, Map.of("safe", true));
        String userAuth = registerAndLogin("user@example.com", "USER");
        String adminAuth = registerAndLogin("admin@example.com", "ADMIN");

        mockMvc.perform(get("/api/v1/admin/outbox").header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/outbox?status=PENDING").header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventType").value("URL_CREATED"))
                .andExpect(jsonPath("$.content[0].eventId").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("payload"))));
    }

    private OutboxEventHandler failingHandler(AtomicInteger attempts) {
        return new OutboxEventHandler() {
            @Override
            public String handlerName() {
                return "failing";
            }

            @Override
            public boolean supports(OutboxEventType eventType, int eventVersion) {
                return true;
            }

            @Override
            public void handle(OutboxEventRecord event) {
                attempts.incrementAndGet();
                throw OutboxHandlingException.transientFailure("temporary_failure", null);
            }
        };
    }

    private UUID publish(OutboxEventType type, Object payload) {
        UUID id = UUID.randomUUID();
        store.insert(new DomainEvent(id, null, "TEST", UUID.randomUUID().toString(), type, 1, payload, LocalDateTime.now(clock)));
        return id;
    }

    private List<String> statuses() {
        return jdbcTemplate.queryForList("select status from outbox_events order by created_at", String.class);
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }

    private String registerAndLogin(String email, String role) throws Exception {
        if ("ADMIN".equals(role)) {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update("""
                            insert into users (id, email, password_hash, role, status, created_at, updated_at)
                            values (?, ?, ?, 'ADMIN', 'ACTIVE', now(), now())
                            """,
                    id,
                    email,
                    passwordEncoder.encode("correct-horse-password"));
        } else {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                    .andExpect(status().isCreated());
        }
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correct-horse-password"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }
}
