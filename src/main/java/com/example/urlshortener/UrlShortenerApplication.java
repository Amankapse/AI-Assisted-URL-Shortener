package com.example.urlshortener;

import com.example.urlshortener.apikey.config.ApiKeyProperties;
import com.example.urlshortener.audit.config.AuditProperties;
import com.example.urlshortener.analytics.config.AnalyticsProperties;
import com.example.urlshortener.idempotency.config.IdempotencyProperties;
import com.example.urlshortener.common.ratelimit.RateLimitProperties;
import com.example.urlshortener.outbox.config.OutboxProperties;
import com.example.urlshortener.redirect.config.RedirectCacheProperties;
import com.example.urlshortener.url.config.AppUrlProperties;
import com.example.urlshortener.url.config.ShortCodeProperties;
import com.example.urlshortener.url.config.UrlOrganizationProperties;
import com.example.urlshortener.url.config.UrlQuotaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        RedirectCacheProperties.class,
        AnalyticsProperties.class,
        AppUrlProperties.class,
        UrlOrganizationProperties.class,
        IdempotencyProperties.class,
        RateLimitProperties.class,
        OutboxProperties.class,
        ApiKeyProperties.class,
        AuditProperties.class,
        ShortCodeProperties.class,
        UrlQuotaProperties.class
})
public class UrlShortenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
