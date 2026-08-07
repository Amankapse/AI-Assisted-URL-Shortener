package com.example.urlshortener.url.service;

import com.example.urlshortener.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class UrlValidationService {

    private static final Set<String> RESERVED_HOSTS = Set.of("localhost", "127.0.0.1", "::1");
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile("^(10\\.|172\\.(1[6-9]|2\\d|3[0-1])\\.|192\\.168\\.|169\\.254\\.).*");
    private static final Set<String> RESERVED_ALIASES = Set.of("api", "r", "actuator", "health", "metrics", "swagger", "docs", "admin");

    public void validateOriginalUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new BadRequestException("originalUrl is required");
        }
        if (originalUrl.length() > 2048) {
            throw new BadRequestException("originalUrl may not exceed 2048 characters");
        }
        URI uri = parseUri(originalUrl);
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BadRequestException("originalUrl must use http or https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BadRequestException("originalUrl must contain a valid host");
        }
        validateHost(host);
    }

    public void validateCustomAlias(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            return;
        }
        if (customAlias.length() > 100) {
            throw new BadRequestException("customAlias may not exceed 100 characters");
        }
        if (!customAlias.matches("^[A-Za-z0-9_-]+$")) {
            throw new BadRequestException("customAlias may contain only letters, digits, hyphen, and underscore");
        }
        if (RESERVED_ALIASES.contains(customAlias.toLowerCase())) {
            throw new BadRequestException("customAlias is reserved");
        }
    }

    public void validateExpiration(String customAlias, java.time.LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new BadRequestException("expiresAt is required");
        }
        if (!expiresAt.isAfter(java.time.LocalDateTime.now())) {
            throw new BadRequestException("expiresAt must be a future timestamp");
        }
    }

    private void validateHost(String host) {
        String normalized = host.toLowerCase();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (RESERVED_HOSTS.contains(normalized)) {
            throw new BadRequestException("originalUrl may not point to localhost or loopback");
        }
        if (PRIVATE_IP_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("originalUrl may not point to private or internal IP addresses");
        }
    }

    private URI parseUri(String originalUrl) {
        try {
            return new URI(originalUrl);
        } catch (URISyntaxException e) {
            throw new BadRequestException("originalUrl is not a valid URI");
        }
    }
}
