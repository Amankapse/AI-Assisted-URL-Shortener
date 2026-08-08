package com.example.urlshortener.apikey.entity;

import com.example.urlshortener.common.exception.BadRequestException;

import java.util.Arrays;
import java.util.Locale;

public enum ApiKeyScope {
    LINKS_READ("links:read"),
    LINKS_WRITE("links:write"),
    ANALYTICS_READ("analytics:read");

    private final String value;

    ApiKeyScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ApiKeyScope fromValue(String value) {
        return Arrays.stream(values())
                .filter(scope -> scope.value.equals(value == null ? null : value.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported API key scope"));
    }
}
