package com.example.urlshortener.common.metrics;

import com.example.urlshortener.analytics.service.AnalyticsCounters;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsBindingConfig {
    public MetricsBindingConfig(MeterRegistry registry, AnalyticsCounters counters) {
        FunctionCounter.builder("url_shortener.analytics.events", counters, AnalyticsCounters::getAcceptedEvents)
                .tag("outcome", "accepted")
                .register(registry);
        FunctionCounter.builder("url_shortener.analytics.events", counters, AnalyticsCounters::getPersistedEvents)
                .tag("outcome", "persisted")
                .register(registry);
        FunctionCounter.builder("url_shortener.analytics.events", counters, AnalyticsCounters::getDroppedEvents)
                .tag("outcome", "dropped")
                .register(registry);
        FunctionCounter.builder("url_shortener.analytics.events", counters, AnalyticsCounters::getFailedEvents)
                .tag("outcome", "failed")
                .register(registry);
        FunctionCounter.builder("url_shortener.analytics.batch.retries", counters, AnalyticsCounters::getBatchRetries)
                .register(registry);
        Gauge.builder("url_shortener.analytics.queue.depth", counters, AnalyticsCounters::getQueueDepth)
                .register(registry);
    }
}
