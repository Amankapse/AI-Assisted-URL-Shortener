package com.example.urlshortener.redirect.cache;

import com.example.urlshortener.redirect.config.RedirectCacheProperties;
import com.example.urlshortener.redirect.service.RedirectTarget;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SingleFlightRedirectLoaderTests {
    @Test
    void concurrentMissShouldUseSingleLeaderAndCleanup() throws Exception {
        RedirectCacheProperties properties = new RedirectCacheProperties();
        properties.setSingleFlightTimeout(Duration.ofSeconds(2));
        SingleFlightRedirectLoader loader = new SingleFlightRedirectLoader(properties);
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        RedirectTarget target = target();

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> loader.load("abc1234", () -> {
                loads.incrementAndGet();
                started.countDown();
                await(release);
                return target;
            }, this::target));
            started.await();
            var second = executor.submit(() -> loader.load("abc1234", () -> {
                loads.incrementAndGet();
                return target;
            }, this::target));
            Thread.sleep(50);
            release.countDown();

            assertThat(first.get()).isEqualTo(target);
            assertThat(second.get()).isEqualTo(target);
        }
        assertThat(loads.get()).isEqualTo(1);
        assertThat(loader.inFlightSize()).isZero();
    }

    @Test
    void timeoutFailureAndCapacityShouldFallbackAndCleanup() {
        RedirectCacheProperties properties = new RedirectCacheProperties();
        properties.setSingleFlightTimeout(Duration.ofMillis(1));
        properties.setSingleFlightCapacity(0);
        SingleFlightRedirectLoader capacityLoader = new SingleFlightRedirectLoader(properties);
        RedirectTarget fallback = target();

        assertThat(capacityLoader.load("abc1234", () -> {
            throw new AssertionError("leader should not run when capacity is exhausted");
        }, () -> fallback)).isEqualTo(fallback);
        assertThat(capacityLoader.inFlightSize()).isZero();

        properties.setSingleFlightCapacity(10);
        SingleFlightRedirectLoader failureLoader = new SingleFlightRedirectLoader(properties);
        assertThatThrownBy(() -> failureLoader.load("abc1234", () -> {
            throw new IllegalStateException("load failed");
        }, () -> fallback)).isInstanceOf(IllegalStateException.class);
        assertThat(failureLoader.inFlightSize()).isZero();
    }

    @Test
    void followerTimeoutShouldFallbackWithoutRemovingLeaderFuture() throws Exception {
        RedirectCacheProperties properties = new RedirectCacheProperties();
        properties.setSingleFlightTimeout(Duration.ofMillis(1));
        SingleFlightRedirectLoader loader = new SingleFlightRedirectLoader(properties);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        RedirectTarget leaderTarget = target();
        RedirectTarget fallback = target();

        try (var executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(() -> loader.load("abc1234", () -> {
                started.countDown();
                await(release);
                return leaderTarget;
            }, this::target));
            started.await();

            RedirectTarget timedOut = loader.load("abc1234", () -> {
                throw new AssertionError("follower should not become leader");
            }, () -> fallback);

            assertThat(timedOut).isEqualTo(fallback);
            assertThat(loader.inFlightSize()).isEqualTo(1);
            release.countDown();
            assertThat(first.get()).isEqualTo(leaderTarget);
        }
        assertThat(loader.inFlightSize()).isZero();
    }

    private RedirectTarget target() {
        return new RedirectTarget(UUID.randomUUID(), "abc1234", "https://example.com", true, null, false);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
