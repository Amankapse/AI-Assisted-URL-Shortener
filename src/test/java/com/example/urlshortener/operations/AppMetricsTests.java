package com.example.urlshortener.operations;

import com.example.urlshortener.common.metrics.AppMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppMetricsTests {
    @Test
    void applicationCountersShouldIncrementWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AppMetrics metrics = new AppMetrics(registry);

        metrics.urlCreated();
        metrics.urlCreationFailed("alias_conflict");
        metrics.redirect("success");
        metrics.cache("lookup", "hit");
        metrics.singleFlight("wait");
        metrics.auth("login", "failure");
        metrics.rateLimit("login", "rejected");

        assertThat(counter(registry, "url_shortener.urls", "operation", "create", "outcome", "success")).isEqualTo(1.0);
        assertThat(counter(registry, "url_shortener.urls", "operation", "create", "outcome", "failure", "reason", "alias_conflict")).isEqualTo(1.0);
        assertThat(counter(registry, "url_shortener.redirects", "outcome", "success")).isEqualTo(1.0);
        assertThat(counter(registry, "url_shortener.redis.cache", "operation", "lookup", "outcome", "hit")).isEqualTo(1.0);
        assertThat(counter(registry, "url_shortener.redis.singleflight", "outcome", "wait")).isEqualTo(1.0);
        assertThat(counter(registry, "url_shortener.auth", "operation", "login", "outcome", "failure")).isEqualTo(1.0);
        assertThat(counter(registry, "url_shortener.rate_limit", "limiter", "login", "outcome", "rejected")).isEqualTo(1.0);
    }

    private double counter(SimpleMeterRegistry registry, String name, String... tags) {
        return registry.get(name).tags(tags).counter().count();
    }
}
