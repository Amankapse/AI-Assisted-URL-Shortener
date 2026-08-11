package com.example.urlshortener.outbox.service;

import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.outbox.config.OutboxProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxMetricsRefresher {
    private final OutboxEventStore store;
    private final AtomicLong backlog = new AtomicLong();
    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong();

    public OutboxMetricsRefresher(OutboxEventStore store, AppMetrics metrics, OutboxProperties properties) {
        this.store = store;
        metrics.registerOutboxGauges(backlog, oldestPendingAgeSeconds);
    }

    @Scheduled(fixedDelayString = "${app.outbox.metrics-refresh-interval:30s}")
    public void refresh() {
        backlog.set(store.backlog());
        oldestPendingAgeSeconds.set(store.oldestPendingAgeSeconds());
    }
}
