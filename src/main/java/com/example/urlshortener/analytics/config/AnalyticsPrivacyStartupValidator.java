package com.example.urlshortener.analytics.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AnalyticsPrivacyStartupValidator implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsPrivacyStartupValidator.class);

    private final AnalyticsProperties properties;
    private final Environment environment;

    public AnalyticsPrivacyStartupValidator(AnalyticsProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean missingPepper = properties.getIpHashPepper() == null || properties.getIpHashPepper().isBlank();
        if (!missingPepper) {
            return;
        }
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (production) {
            throw new IllegalStateException("APP_ANALYTICS_IP_HASH_PEPPER must be configured in production");
        }
        log.warn("APP_ANALYTICS_IP_HASH_PEPPER is not configured; analytics IP hashes will be omitted outside test/local configuration");
    }
}
