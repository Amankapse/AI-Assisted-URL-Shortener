package com.example.urlshortener.analytics.service;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClickAnalyticsEvent(
        UUID eventId,
        UUID urlId,
        LocalDateTime clickedAt,
        String ipHash,
        String userAgentCategory,
        String referrerHost,
        String correlationId
) {
}
