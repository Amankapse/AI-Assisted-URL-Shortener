package com.example.urlshortener.operations;

import com.example.urlshortener.common.ratelimit.RateLimitExceededException;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperationalWebIntegrationTests {
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Autowired MockMvc mockMvc;
    @Autowired MeterRegistry meterRegistry;
    @MockBean RateLimiterService rateLimiter;

    @Test
    void rateLimitResponseShouldUseRfc7807WithoutInternalState() throws Exception {
        doThrow(new RateLimitExceededException("registration", 12))
                .when(rateLimiter).enforce(eq("registration"), anyString());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rate@example.com\",\"password\":\"correct-horse-password\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "12"))
                .andExpect(jsonPath("$.type").value("https://example.com/problem/rate-limit-exceeded"))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.errorCode").value("rate_limit_exceeded"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("rl:v1"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("registration:"))));
    }

    @Test
    void loginRateLimitShouldRemainGenericAndNotRevealAccountExistence() throws Exception {
        doThrow(new RateLimitExceededException("login", 30))
                .when(rateLimiter).enforce(eq("login"), anyString());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\",\"password\":\"correct-horse-password\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("rate_limit_exceeded"))
                .andExpect(jsonPath("$.detail").value("Too many requests."))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("unknown@example.com"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("account"))));
    }

    @Test
    void correlationIdShouldBeGeneratedSanitizedPropagatedAndReturnedInProblemDetails() throws Exception {
        String body = mockMvc.perform(get("/api/v1/urls")
                        .header("X-Correlation-ID", "bad value with spaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String header = mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-ID", "safe-id_123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "safe-id_123"))
                .andReturn()
                .getResponse()
                .getHeader("X-Correlation-ID");

        assertThat(header).isEqualTo("safe-id_123");
        String generated = body.replaceAll(".*\"correlationId\":\"([^\"]+)\".*", "$1");
        assertThat(generated).matches(SAFE_CORRELATION_ID);
        assertThat(generated).isNotEqualTo("bad value with spaces");
    }

    @Test
    void securityHeadersAndActuatorExposureShouldMatchPolicy() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metersShouldAvoidSensitiveHighCardinalityTags() {
        Set<String> forbidden = Set.of("userId", "urlId", "shortCode", "email", "ip", "tokenId", "exceptionMessage");

        assertThat(meterRegistry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .allSatisfy(tag -> assertThat(forbidden).doesNotContain(tag.getKey())));
    }
}
