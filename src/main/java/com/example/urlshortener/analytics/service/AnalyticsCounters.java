package com.example.urlshortener.analytics.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class AnalyticsCounters {
    private final AtomicLong acceptedEvents = new AtomicLong();
    private final AtomicLong droppedEvents = new AtomicLong();
    private final AtomicLong persistedEvents = new AtomicLong();
    private final AtomicLong failedEvents = new AtomicLong();
    private final AtomicLong batchRetries = new AtomicLong();
    private final AtomicLong queueDepth = new AtomicLong();

    public void accepted() { acceptedEvents.incrementAndGet(); }
    public void dropped() { droppedEvents.incrementAndGet(); }
    public void persisted(long count) { persistedEvents.addAndGet(count); }
    public void failed(long count) { failedEvents.addAndGet(count); }
    public void retried() { batchRetries.incrementAndGet(); }
    public void queueDepth(long depth) { queueDepth.set(depth); }

    public long getAcceptedEvents() { return acceptedEvents.get(); }
    public long getDroppedEvents() { return droppedEvents.get(); }
    public long getPersistedEvents() { return persistedEvents.get(); }
    public long getFailedEvents() { return failedEvents.get(); }
    public long getBatchRetries() { return batchRetries.get(); }
    public long getQueueDepth() { return queueDepth.get(); }
}
