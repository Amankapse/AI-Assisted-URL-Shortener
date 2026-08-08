package com.example.urlshortener.redirect.service;

import com.example.urlshortener.analytics.service.ClickEventPublisher;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.common.web.ClientIpResolver;
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
    private final ClickEventPublisher analyticsPublisher;
    private final Clock clock;
    private final RateLimiterService rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AppMetrics metrics;

    public RedirectService(UrlService urlService,
                           RedirectCacheService cacheService,
                           SingleFlightRedirectLoader singleFlightLoader,
                           ClickEventPublisher analyticsPublisher,
                           Clock clock,
                           RateLimiterService rateLimiter,
                           ClientIpResolver clientIpResolver,
                           AppMetrics metrics) {
        this.urlService = urlService;
        this.cacheService = cacheService;
        this.singleFlightLoader = singleFlightLoader;
        this.analyticsPublisher = analyticsPublisher;
        this.clock = clock;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
        this.metrics = metrics;
    }

    public RedirectTarget resolve(String shortCode, HttpServletRequest request) {
        rateLimiter.enforce("redirect", clientIpResolver.resolve(request));
        try {
            RedirectTarget target = cacheService.get(shortCode)
                    .orElseGet(() -> singleFlightLoader.load(
                            shortCode,
                            () -> cacheService.get(shortCode).orElseGet(() -> loadAndCache(shortCode)),
                            () -> urlService.resolveRedirectTarget(shortCode)
                    ));
            validateEligible(target);
            analyticsPublisher.publish(target, request);
            metrics.redirect("success");
            return target;
        } catch (ResourceNotFoundException ex) {
            metrics.redirect("not_found");
            throw ex;
        } catch (BadRequestException ex) {
            metrics.redirect(reason(ex));
            throw ex;
        }
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
        if (target.blocked()) {
            throw new ResourceNotFoundException("Short URL not found");
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

    private String reason(BadRequestException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(java.util.Locale.ROOT);
        if (message.contains("deleted")) {
            return "deleted";
        }
        if (message.contains("blocked")) {
            return "blocked";
        }
        if (message.contains("disabled")) {
            return "disabled";
        }
        if (message.contains("expired")) {
            return "expired";
        }
        return "invalid";
    }
}
