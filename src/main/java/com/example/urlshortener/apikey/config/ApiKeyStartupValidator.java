package com.example.urlshortener.apikey.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ApiKeyStartupValidator {
    private final ApiKeyProperties properties;
    private final Environment environment;

    public ApiKeyStartupValidator(ApiKeyProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod && (properties.getHashPepper() == null || properties.getHashPepper().isBlank())) {
            throw new IllegalStateException("APP_API_KEY_HASH_PEPPER is required in production");
        }
        if (properties.getMaxHeaderLength() < 64 || properties.getMaxHeaderLength() > 1024) {
            throw new IllegalStateException("API key max header length must be between 64 and 1024");
        }
    }
}
