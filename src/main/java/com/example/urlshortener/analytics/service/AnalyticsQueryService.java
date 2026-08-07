package com.example.urlshortener.analytics.service;

import com.example.urlshortener.analytics.config.AnalyticsProperties;
import com.example.urlshortener.analytics.dto.AnalyticsDtos.AdminAnalyticsOverviewResponse;
import com.example.urlshortener.analytics.dto.AnalyticsDtos.DailyRedirects;
import com.example.urlshortener.analytics.dto.AnalyticsDtos.DailyRedirectsResponse;
import com.example.urlshortener.analytics.dto.AnalyticsDtos.TopLinkResponse;
import com.example.urlshortener.analytics.dto.AnalyticsDtos.UrlAnalyticsResponse;
import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsQueryService {
    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final UserRepository userRepository;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final AnalyticsProperties properties;
    private final Clock clock;
    private final RateLimiterService rateLimiter;

    public AnalyticsQueryService(ShortUrlRepository shortUrlRepository,
                                 ClickEventRepository clickEventRepository,
                                 UserRepository userRepository,
                                 CurrentOwnerProvider currentOwnerProvider,
                                 AnalyticsProperties properties,
                                 Clock clock,
                                 RateLimiterService rateLimiter) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.userRepository = userRepository;
        this.currentOwnerProvider = currentOwnerProvider;
        this.properties = properties;
        this.clock = clock;
        this.rateLimiter = rateLimiter;
    }

    @Transactional(readOnly = true)
    public UrlAnalyticsResponse urlAnalytics(UUID urlId) {
        ShortUrlEntity url = ownedUrl(urlId);
        return new UrlAnalyticsResponse(
                url.getId(),
                url.getShortCode(),
                state(url),
                clickEventRepository.countByUrl(url),
                clickEventRepository.findLatestClickAt(url)
        );
    }

    @Transactional(readOnly = true)
    public DailyRedirectsResponse dailyAnalytics(UUID urlId) {
        ShortUrlEntity url = ownedUrl(urlId);
        List<DailyRedirects> days = clickEventRepository.countDailyByUrlId(url.getId()).stream()
                .map(row -> new DailyRedirects(row.getDay(), row.getRedirects()))
                .toList();
        return new DailyRedirectsResponse(url.getId(), days);
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsOverviewResponse adminOverview() {
        rateLimiter.enforce("admin-analytics", currentOwnerProvider.getCurrentOwner().userId().toString());
        return new AdminAnalyticsOverviewResponse(
                userRepository.count(),
                shortUrlRepository.countNotDeleted(),
                shortUrlRepository.countActiveLinks(),
                shortUrlRepository.countByEnabled(false),
                shortUrlRepository.countExpiredLinks(),
                clickEventRepository.countAllRedirects()
        );
    }

    @Transactional(readOnly = true)
    public List<TopLinkResponse> topLinks(int limit) {
        rateLimiter.enforce("admin-analytics", currentOwnerProvider.getCurrentOwner().userId().toString());
        int boundedLimit = Math.max(1, Math.min(limit, properties.getTopLinksMax()));
        return clickEventRepository.findTopLinks(PageRequest.of(0, boundedLimit)).stream()
                .map(row -> new TopLinkResponse(row.getShortCode(), row.getRedirects()))
                .toList();
    }

    private ShortUrlEntity ownedUrl(UUID urlId) {
        UserEntity owner = userRepository.getReferenceById(currentOwnerProvider.getCurrentOwner().userId());
        return shortUrlRepository.findByIdAndOwner(urlId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
    }

    private String state(ShortUrlEntity url) {
        if (!url.isEnabled()) {
            return "disabled";
        }
        if (url.getExpiresAt() != null && !url.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            return "expired";
        }
        return "active";
    }
}
