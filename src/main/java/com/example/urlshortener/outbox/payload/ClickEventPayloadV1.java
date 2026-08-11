package com.example.urlshortener.outbox.payload;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClickEventPayloadV1(
        UUID eventId,
        UUID urlId,
        LocalDateTime clickedAt,
        String ipHash,
        String userAgentCategory,
        String referrerHost,
        String correlationId
) {
}
