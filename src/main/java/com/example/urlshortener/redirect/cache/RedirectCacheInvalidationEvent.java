package com.example.urlshortener.redirect.cache;

import java.util.Set;

public record RedirectCacheInvalidationEvent(Set<String> shortCodes) {
    public RedirectCacheInvalidationEvent(String shortCode) {
        this(Set.of(shortCode));
    }
}
