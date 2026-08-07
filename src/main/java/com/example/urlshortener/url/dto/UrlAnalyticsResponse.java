package com.example.urlshortener.url.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class UrlAnalyticsResponse {

    private UUID urlId;
    private long totalClicks;
    private LocalDateTime latestClickAt;

    public UrlAnalyticsResponse() {
    }

    public UrlAnalyticsResponse(UUID urlId, long totalClicks, LocalDateTime latestClickAt) {
        this.urlId = urlId;
        this.totalClicks = totalClicks;
        this.latestClickAt = latestClickAt;
    }

    public UUID getUrlId() {
        return urlId;
    }

    public void setUrlId(UUID urlId) {
        this.urlId = urlId;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public LocalDateTime getLatestClickAt() {
        return latestClickAt;
    }

    public void setLatestClickAt(LocalDateTime latestClickAt) {
        this.latestClickAt = latestClickAt;
    }
}
