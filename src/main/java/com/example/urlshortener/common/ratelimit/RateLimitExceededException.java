package com.example.urlshortener.common.ratelimit;

public class RateLimitExceededException extends RuntimeException {
    private final String limiter;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String limiter, long retryAfterSeconds) {
        super("Too many requests");
        this.limiter = limiter;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String limiter() {
        return limiter;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
