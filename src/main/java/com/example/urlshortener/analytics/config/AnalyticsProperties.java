package com.example.urlshortener.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.analytics")
public class AnalyticsProperties {
    private String ipHashPepper = "";
    private int queueCapacity = 1000;
    private int batchSize = 100;
    private Duration flushInterval = Duration.ofSeconds(1);
    private Duration offerTimeout = Duration.ofMillis(10);
    private Duration shutdownFlushTimeout = Duration.ofSeconds(5);
    private int retryCount = 2;
    private int topLinksMax = 25;

    public String getIpHashPepper() {
        return ipHashPepper;
    }

    public void setIpHashPepper(String ipHashPepper) {
        this.ipHashPepper = ipHashPepper;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(Duration flushInterval) {
        this.flushInterval = flushInterval;
    }

    public Duration getOfferTimeout() {
        return offerTimeout;
    }

    public void setOfferTimeout(Duration offerTimeout) {
        this.offerTimeout = offerTimeout;
    }

    public Duration getShutdownFlushTimeout() {
        return shutdownFlushTimeout;
    }

    public void setShutdownFlushTimeout(Duration shutdownFlushTimeout) {
        this.shutdownFlushTimeout = shutdownFlushTimeout;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getTopLinksMax() {
        return topLinksMax;
    }

    public void setTopLinksMax(int topLinksMax) {
        this.topLinksMax = topLinksMax;
    }
}
