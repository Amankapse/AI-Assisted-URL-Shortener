package com.example.urlshortener;

import com.example.urlshortener.analytics.config.AnalyticsProperties;
import com.example.urlshortener.common.ratelimit.RateLimitProperties;
import com.example.urlshortener.redirect.config.RedirectCacheProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RedirectCacheProperties.class, AnalyticsProperties.class, RateLimitProperties.class})
public class UrlShortenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
