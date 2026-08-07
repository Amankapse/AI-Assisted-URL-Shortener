package com.example.urlshortener.common.ratelimit;

public record RateLimitResult(boolean allowed, long retryAfterSeconds, boolean degraded) {
    public static RateLimitResult allow() {
        return new RateLimitResult(true, 0, false);
    }

    public static RateLimitResult degradedAllow() {
        return new RateLimitResult(true, 0, true);
    }

    public static RateLimitResult reject(long retryAfterSeconds) {
        return new RateLimitResult(false, Math.max(1, retryAfterSeconds), false);
    }
}
