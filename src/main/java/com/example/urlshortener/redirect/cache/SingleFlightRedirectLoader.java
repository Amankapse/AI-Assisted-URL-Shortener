package com.example.urlshortener.redirect.cache;

import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.redirect.config.RedirectCacheProperties;
import com.example.urlshortener.redirect.service.RedirectTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.function.Supplier;

@Component
public class SingleFlightRedirectLoader {
    private static final Logger log = LoggerFactory.getLogger(SingleFlightRedirectLoader.class);

    private final ConcurrentHashMap<String, CompletableFuture<RedirectTarget>> inFlight = new ConcurrentHashMap<>();
    private final RedirectCacheProperties properties;
    private final AppMetrics metrics;

    public SingleFlightRedirectLoader(RedirectCacheProperties properties, AppMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
    }

    public RedirectTarget load(String shortCode, Supplier<RedirectTarget> leaderLoad, Supplier<RedirectTarget> fallbackLoad) {
        if (inFlight.size() >= properties.getSingleFlightCapacity()) {
            log.warn("Redirect single-flight capacity reached; using direct PostgreSQL fallback");
            metrics.singleFlight("capacity_fallback");
            return fallbackLoad.get();
        }

        CompletableFuture<RedirectTarget> candidate = new CompletableFuture<>();
        CompletableFuture<RedirectTarget> existing = inFlight.putIfAbsent(shortCode, candidate);
        if (existing == null) {
            metrics.singleFlight("leader");
            try {
                RedirectTarget target = leaderLoad.get();
                candidate.complete(target);
                return target;
            } catch (Throwable ex) {
                candidate.completeExceptionally(ex);
                throw ex;
            } finally {
                inFlight.remove(shortCode, candidate);
            }
        }

        try {
            metrics.singleFlight("wait");
            return existing.get(properties.getSingleFlightTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            metrics.singleFlight("fallback");
            return fallbackLoad.get();
        } catch (TimeoutException ex) {
            log.warn("Redirect single-flight wait timed out; using direct PostgreSQL fallback");
            metrics.singleFlight("timeout");
            return fallbackLoad.get();
        } catch (ExecutionException ex) {
            metrics.singleFlight("fallback");
            return fallbackLoad.get();
        }
    }

    public int inFlightSize() {
        return inFlight.size();
    }
}
