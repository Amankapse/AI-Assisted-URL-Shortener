package com.example.urlshortener.redirect.cache;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RedirectCacheInvalidationListener {
    private final RedirectCacheService cacheService;

    public RedirectCacheInvalidationListener(RedirectCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(RedirectCacheInvalidationEvent event) {
        event.shortCodes().forEach(cacheService::evict);
    }
}
