package com.example.urlshortener.url.search;

import java.time.LocalDateTime;
import java.util.UUID;

public record UrlSearchCriteria(
        String q,
        UrlState state,
        UUID campaignId,
        String tag,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        LocalDateTime expiresBefore,
        LocalDateTime expiresAfter,
        Boolean customAlias,
        String sort,
        int page,
        int size
) {
}
