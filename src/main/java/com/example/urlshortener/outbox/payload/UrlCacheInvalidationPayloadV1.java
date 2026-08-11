package com.example.urlshortener.outbox.payload;

import java.util.UUID;

public record UrlCacheInvalidationPayloadV1(
        UUID urlId,
        String shortCode
) {
}
