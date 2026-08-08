package com.example.urlshortener.apikey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.api-key")
public class ApiKeyProperties {
    private String hashPepper = "local-development-api-key-pepper";
    private Duration defaultExpiry = Duration.ofDays(90);
    private Duration maxExpiry = Duration.ofDays(365);
    private Duration lastUsedUpdateInterval = Duration.ofMinutes(15);
    private int maxHeaderLength = 256;

    public String getHashPepper() {
        return hashPepper;
    }

    public void setHashPepper(String hashPepper) {
        this.hashPepper = hashPepper;
    }

    public Duration getDefaultExpiry() {
        return defaultExpiry;
    }

    public void setDefaultExpiry(Duration defaultExpiry) {
        this.defaultExpiry = defaultExpiry;
    }

    public Duration getMaxExpiry() {
        return maxExpiry;
    }

    public void setMaxExpiry(Duration maxExpiry) {
        this.maxExpiry = maxExpiry;
    }

    public Duration getLastUsedUpdateInterval() {
        return lastUsedUpdateInterval;
    }

    public void setLastUsedUpdateInterval(Duration lastUsedUpdateInterval) {
        this.lastUsedUpdateInterval = lastUsedUpdateInterval;
    }

    public int getMaxHeaderLength() {
        return maxHeaderLength;
    }

    public void setMaxHeaderLength(int maxHeaderLength) {
        this.maxHeaderLength = maxHeaderLength;
    }
}
