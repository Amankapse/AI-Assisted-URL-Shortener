package com.example.urlshortener.url.service;

import com.example.urlshortener.url.config.AppUrlProperties;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlBuilder {
    private final AppUrlProperties properties;

    public PublicUrlBuilder(AppUrlProperties properties) {
        this.properties = properties;
    }

    public String shortUrl(String shortCode) {
        return normalizedBaseUrl() + "/r/" + shortCode;
    }

    private String normalizedBaseUrl() {
        String baseUrl = properties.getPublicBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
