package com.example.urlshortener.redirect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.redirect.cache")
public class RedirectCacheProperties {
    private boolean enabled = true;
    private Duration ttl = Duration.ofMinutes(10);
    private Duration ineligibleTtl = Duration.ofSeconds(30);
    private Duration jitter = Duration.ofSeconds(30);
    private Duration singleFlightTimeout = Duration.ofSeconds(2);
    private int singleFlightCapacity = 1024;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getIneligibleTtl() {
        return ineligibleTtl;
    }

    public void setIneligibleTtl(Duration ineligibleTtl) {
        this.ineligibleTtl = ineligibleTtl;
    }

    public Duration getJitter() {
        return jitter;
    }

    public void setJitter(Duration jitter) {
        this.jitter = jitter;
    }

    public Duration getSingleFlightTimeout() {
        return singleFlightTimeout;
    }

    public void setSingleFlightTimeout(Duration singleFlightTimeout) {
        this.singleFlightTimeout = singleFlightTimeout;
    }

    public int getSingleFlightCapacity() {
        return singleFlightCapacity;
    }

    public void setSingleFlightCapacity(int singleFlightCapacity) {
        this.singleFlightCapacity = singleFlightCapacity;
    }
}
