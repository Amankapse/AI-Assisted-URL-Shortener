package com.example.urlshortener.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AppMetrics {
    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final Timer analyticsBatchTimer;
    private final DistributionSummary analyticsBatchSize;

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.analyticsBatchTimer = Timer.builder("url_shortener.analytics.batch.persistence")
                .description("Analytics batch persistence latency")
                .register(registry);
        this.analyticsBatchSize = DistributionSummary.builder("url_shortener.analytics.batch.size")
                .description("Analytics batch size")
                .baseUnit("events")
                .register(registry);
    }

    public void urlCreated() {
        increment("url_shortener.urls", "operation", "create", "outcome", "success");
    }

    public void urlCreationFailed(String reason) {
        increment("url_shortener.urls", "operation", "create", "outcome", "failure", "reason", reason);
    }

    public void shortCodeGeneration(String outcome) {
        increment("url_shortener.short_code.generation", "outcome", outcome);
    }

    public void quota(String quota, String outcome) {
        increment("url_shortener.quota", "quota", quota, "outcome", outcome);
    }

    public void redirect(String outcome) {
        increment("url_shortener.redirects", "outcome", outcome);
    }

    public void cache(String operation, String outcome) {
        increment("url_shortener.redis.cache", "operation", operation, "outcome", outcome);
    }

    public void singleFlight(String outcome) {
        increment("url_shortener.redis.singleflight", "outcome", outcome);
    }

    public void auth(String operation, String outcome) {
        increment("url_shortener.auth", "operation", operation, "outcome", outcome);
    }

    public void rateLimit(String limiter, String outcome) {
        increment("url_shortener.rate_limit", "limiter", limiter, "outcome", outcome);
    }

    public void idempotency(String outcome) {
        increment("url_shortener.idempotency", "outcome", outcome);
    }

    public void outbox(String operation, String eventType, String handler, String outcome) {
        increment("url_shortener.outbox." + operation, "eventType", eventType, "handler", handler, "outcome", outcome);
    }

    public void campaign(String operation, String outcome) {
        increment("url_shortener.campaigns", "operation", operation, "outcome", outcome);
    }

    public void tags(String operation, String outcome) {
        increment("url_shortener.tags", "operation", operation, "outcome", outcome);
    }

    public void urlSearch(String outcome) {
        increment("url_shortener.urls.search", "outcome", outcome);
    }

    public void recordUrlSearch(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("url_shortener.urls.search.latency")
                .tags("outcome", outcome)
                .register(registry));
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordAnalyticsBatch(Timer.Sample sample, int size) {
        analyticsBatchSize.record(size);
        sample.stop(analyticsBatchTimer);
    }

    public void recordOutboxDispatch(Timer.Sample sample, String eventType, String outcome) {
        sample.stop(Timer.builder("url_shortener.outbox.dispatch_latency")
                .tags("eventType", eventType, "outcome", outcome)
                .register(registry));
    }

    public void recordOutboxHandler(Timer.Sample sample, String handler, String outcome) {
        sample.stop(Timer.builder("url_shortener.outbox.handler_latency")
                .tags("handler", handler, "outcome", outcome)
                .register(registry));
    }

    public void registerOutboxGauges(AtomicLong backlog, AtomicLong oldestPendingAgeSeconds) {
        io.micrometer.core.instrument.Gauge.builder("url_shortener.outbox.backlog", backlog, AtomicLong::get)
                .description("Cached count of pending or processing outbox events")
                .register(registry);
        io.micrometer.core.instrument.Gauge.builder("url_shortener.outbox.oldest_pending_age", oldestPendingAgeSeconds, AtomicLong::get)
                .description("Cached age of oldest pending or processing outbox event")
                .baseUnit("seconds")
                .register(registry);
    }

    private void increment(String name, String... tags) {
        counters.computeIfAbsent(key(name, tags), ignored -> Counter.builder(name).tags(tags).register(registry)).increment();
    }

    private String key(String name, String[] tags) {
        return name + "|" + String.join("|", tags);
    }
}
