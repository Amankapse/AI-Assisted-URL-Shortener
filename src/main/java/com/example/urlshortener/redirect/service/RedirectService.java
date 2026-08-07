package com.example.urlshortener.redirect.service;

import com.example.urlshortener.analytics.service.ClickAnalyticsPublisher;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.redirect.cache.RedirectCacheService;
import com.example.urlshortener.redirect.cache.SingleFlightRedirectLoader;
import com.example.urlshortener.url.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class RedirectService {

    private final UrlService urlService;
    private final RedirectCacheService cacheService;
    private final SingleFlightRedirectLoader singleFlightLoader;
    private final ClickAnalyticsPublisher analyticsPublisher;
    private final Clock clock;

    public RedirectService(UrlService urlService,
                           RedirectCacheService cacheService,
                           SingleFlightRedirectLoader singleFlightLoader,
                           ClickAnalyticsPublisher analyticsPublisher,
                           Clock clock) {
        this.urlService = urlService;
        this.cacheService = cacheService;
        this.singleFlightLoader = singleFlightLoader;
        this.analyticsPublisher = analyticsPublisher;
        this.clock = clock;
    }

    public RedirectTarget resolve(String shortCode, HttpServletRequest request) {
        RedirectTarget target = cacheService.get(shortCode)
                .orElseGet(() -> singleFlightLoader.load(
                        shortCode,
                        () -> cacheService.get(shortCode).orElseGet(() -> loadAndCache(shortCode)),
                        () -> urlService.resolveRedirectTarget(shortCode)
                ));
        validateEligible(target);
        analyticsPublisher.publish(target, request);
        return target;
    }

    private RedirectTarget loadAndCache(String shortCode) {
        RedirectTarget target = urlService.resolveRedirectTarget(shortCode);
        cacheService.put(target);
        return target;
    }

    private void validateEligible(RedirectTarget target) {
        if (target.deleted()) {
            throw new BadRequestException("Short URL has been deleted");
        }
        if (!target.enabled()) {
            throw new BadRequestException("Short URL is disabled");
        }
        if (target.expiresAt() != null && !target.expiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Short URL has expired");
        }
        if (target.destinationUrl() == null || target.destinationUrl().isBlank()) {
            throw new BadRequestException("Short URL destination is unavailable");
        }
    }
}
