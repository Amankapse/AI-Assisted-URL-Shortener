package com.example.urlshortener.analytics.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }

    public record UrlAnalyticsResponse(
            UUID urlId,
            String shortCode,
            String state,
            long totalRedirects,
            LocalDateTime lastAccessedAt
    ) {
    }

    public record DailyRedirectsResponse(
            UUID urlId,
            List<DailyRedirects> days
    ) {
    }

    public record DailyRedirects(LocalDate day, long redirects) {
    }

    public record AdminAnalyticsOverviewResponse(
            long totalUsers,
            long totalLinks,
            long activeLinks,
            long disabledLinks,
            long expiredLinks,
            long totalRedirects
    ) {
    }

    public record TopLinkResponse(String shortCode, long redirects) {
    }
}
