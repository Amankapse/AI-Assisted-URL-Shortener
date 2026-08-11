package com.example.urlshortener.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.audit")
public class AuditProperties {
    private int metadataMaxBytes = 4096;
    private Duration retention = Duration.ofDays(3650);

    public int getMetadataMaxBytes() {
        return metadataMaxBytes;
    }

    public void setMetadataMaxBytes(int metadataMaxBytes) {
        this.metadataMaxBytes = metadataMaxBytes;
    }

    public Duration getRetention() {
        return retention;
    }

    public void setRetention(Duration retention) {
        this.retention = retention;
    }
}
