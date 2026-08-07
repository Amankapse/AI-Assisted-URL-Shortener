package com.example.urlshortener.analytics.service;

import com.example.urlshortener.analytics.config.AnalyticsProperties;
import com.example.urlshortener.redirect.service.RedirectTarget;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ClickAnalyticsPublisher implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(ClickAnalyticsPublisher.class);

    private final BlockingQueue<ClickAnalyticsEvent> queue;
    private final AnalyticsProperties properties;
    private final ClickPrivacySanitizer sanitizer;
    private final ClickAnalyticsWriter writer;
    private final AnalyticsCounters counters;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    public ClickAnalyticsPublisher(AnalyticsProperties properties,
                                   ClickPrivacySanitizer sanitizer,
                                   ClickAnalyticsWriter writer,
                                   AnalyticsCounters counters,
                                   Clock clock) {
        this.properties = properties;
        this.sanitizer = sanitizer;
        this.writer = writer;
        this.counters = counters;
        this.clock = clock;
        this.queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
    }

    public void publish(RedirectTarget target, HttpServletRequest request) {
        ClickAnalyticsEvent event = new ClickAnalyticsEvent(
                UUID.randomUUID(),
                target.urlId(),
                LocalDateTime.now(clock),
                sanitizer.ipHash(request),
                sanitizer.userAgentCategory(request),
                sanitizer.referrerHost(request),
                sanitizer.correlationId(request)
        );
        try {
            boolean accepted = queue.offer(event, properties.getOfferTimeout().toMillis(), TimeUnit.MILLISECONDS);
            counters.queueDepth(queue.size());
            if (accepted) {
                counters.accepted();
            } else {
                counters.dropped();
                log.warn("Analytics event dropped because the bounded queue is full");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            counters.dropped();
            log.warn("Analytics event dropped because enqueue was interrupted");
        } catch (RuntimeException ex) {
            counters.dropped();
            log.warn("Analytics event dropped during safe request sanitization");
        }
    }

    public void flushOnce() {
        List<ClickAnalyticsEvent> batch = new ArrayList<>(properties.getBatchSize());
        queue.drainTo(batch, properties.getBatchSize());
        counters.queueDepth(queue.size());
        if (!batch.isEmpty()) {
            persistWithRetries(batch);
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = new Thread(this::runLoop, "click-analytics-worker");
            worker.setDaemon(true);
            worker.start();
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(properties.getShutdownFlushTimeout().toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        flushUntilDeadline();
    }

    @PreDestroy
    public void shutdown() {
        stop();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void runLoop() {
        while (running.get()) {
            try {
                ClickAnalyticsEvent first = queue.poll(properties.getFlushInterval().toMillis(), TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                List<ClickAnalyticsEvent> batch = new ArrayList<>(properties.getBatchSize());
                batch.add(first);
                queue.drainTo(batch, properties.getBatchSize() - 1);
                counters.queueDepth(queue.size());
                persistWithRetries(batch);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void persistWithRetries(List<ClickAnalyticsEvent> batch) {
        int attempts = 0;
        while (true) {
            try {
                int persisted = writer.persistBatch(batch);
                counters.persisted(persisted);
                return;
            } catch (TransientDataAccessException ex) {
                if (attempts >= properties.getRetryCount()) {
                    counters.failed(batch.size());
                    log.warn("Analytics batch failed after bounded retries");
                    return;
                }
                attempts++;
                counters.retried();
            } catch (RuntimeException ex) {
                counters.failed(batch.size());
                log.warn("Analytics batch failed permanently");
                return;
            }
        }
    }

    private void flushUntilDeadline() {
        long deadline = System.nanoTime() + properties.getShutdownFlushTimeout().toNanos();
        while (!queue.isEmpty() && System.nanoTime() < deadline) {
            flushOnce();
        }
        if (!queue.isEmpty()) {
            long remaining = queue.size();
            counters.failed(remaining);
            log.warn("Analytics shutdown flush timed out with {} queued events", remaining);
        }
    }
}
