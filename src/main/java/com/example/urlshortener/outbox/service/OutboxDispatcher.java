package com.example.urlshortener.outbox.service;

import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.outbox.config.OutboxProperties;
import com.example.urlshortener.outbox.domain.OutboxEventHandler;
import com.example.urlshortener.outbox.domain.OutboxEventRecord;
import com.example.urlshortener.outbox.domain.OutboxFailureType;
import com.example.urlshortener.outbox.domain.OutboxHandlingException;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class OutboxDispatcher implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventStore store;
    private final List<OutboxEventHandler> handlers;
    private final OutboxProperties properties;
    private final OutboxInstanceId instanceId;
    private final AppMetrics metrics;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    public OutboxDispatcher(OutboxEventStore store,
                            List<OutboxEventHandler> handlers,
                            OutboxProperties properties,
                            OutboxInstanceId instanceId,
                            AppMetrics metrics,
                            Clock clock) {
        this.store = store;
        this.handlers = handlers;
        this.properties = properties;
        this.instanceId = instanceId;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public void start() {
        if (!properties.isEnabled()) {
            return;
        }
        if (running.compareAndSet(false, true)) {
            executor = Executors.newFixedThreadPool(Math.max(1, properties.getWorkers()), task -> {
                Thread thread = new Thread(task, "outbox-dispatcher");
                thread.setDaemon(true);
                return thread;
            });
            for (int i = 0; i < Math.max(1, properties.getWorkers()); i++) {
                executor.submit(this::runLoop);
            }
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(properties.getShutdownGracePeriod().toMillis(), TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void runLoop() {
        while (running.get()) {
            try {
                List<OutboxEventRecord> batch = store.claimBatch(instanceId.value());
                if (batch.isEmpty()) {
                    sleep(properties.getPollInterval());
                    continue;
                }
                for (OutboxEventRecord event : batch) {
                    process(event);
                }
            } catch (RuntimeException ex) {
                log.warn("Outbox dispatcher polling failed");
                sleep(properties.getPollInterval());
            }
        }
    }

    public void processOnceForTests() {
        store.claimBatch(instanceId.value()).forEach(this::process);
    }

    private void process(OutboxEventRecord event) {
        metrics.outbox("claimed", event.eventType().name(), "dispatch", "success");
        Timer.Sample dispatchSample = metrics.startTimer();
        OutboxEventHandler handler = handlers.stream()
                .filter(candidate -> candidate.supports(event.eventType(), event.eventVersion()))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            markFailure(event, OutboxFailureType.PERMANENT, "unsupported_event_version");
            return;
        }
        Timer.Sample handlerSample = metrics.startTimer();
        try {
            handler.handle(event);
            metrics.recordOutboxHandler(handlerSample, handler.handlerName(), "success");
            store.markProcessed(event.id());
            metrics.recordOutboxDispatch(dispatchSample, event.eventType().name(), "processed");
            metrics.outbox("processed", event.eventType().name(), handler.handlerName(), "success");
        } catch (OutboxHandlingException ex) {
            metrics.recordOutboxHandler(handlerSample, handler.handlerName(), "failure");
            markFailure(event, ex.failureType(), ex.errorCode());
        } catch (RuntimeException ex) {
            metrics.recordOutboxHandler(handlerSample, handler.handlerName(), "failure");
            markFailure(event, OutboxFailureType.TRANSIENT, "outbox_handler_failure");
        }
    }

    private void markFailure(OutboxEventRecord event, OutboxFailureType type, String errorCode) {
        int nextAttempt = event.attemptCount() + 1;
        boolean dead = type == OutboxFailureType.PERMANENT || nextAttempt >= properties.getMaxAttempts();
        LocalDateTime nextAttemptAt = dead ? LocalDateTime.now(clock) : LocalDateTime.now(clock).plus(backoff(nextAttempt));
        store.markFailed(event.id(), nextAttempt, nextAttemptAt, errorCode, dead);
        metrics.outbox(dead ? "dead" : "failed", event.eventType().name(), "dispatch", errorCode);
        if (!dead) {
            metrics.outbox("retried", event.eventType().name(), "dispatch", "scheduled");
        }
    }

    private Duration backoff(int attempt) {
        long baseMillis = properties.getBaseBackoff().toMillis();
        long maxMillis = properties.getMaxBackoff().toMillis();
        long exponential = baseMillis * (1L << Math.min(20, Math.max(0, attempt - 1)));
        long bounded = Math.min(maxMillis, Math.max(baseMillis, exponential));
        long jitter = bounded <= 1 ? 0 : ThreadLocalRandom.current().nextLong(Math.max(1, bounded / 4));
        return Duration.ofMillis(Math.min(maxMillis, bounded + jitter));
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(Math.max(1, duration.toMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
