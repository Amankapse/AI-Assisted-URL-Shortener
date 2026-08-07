package com.example.urlshortener.analytics.service;

import com.example.urlshortener.analytics.config.AnalyticsProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.util.HexFormat;

@Component
public class ClickPrivacySanitizer {
    private final AnalyticsProperties properties;

    public ClickPrivacySanitizer(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public String ipHash(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank() || properties.getIpHashPepper().isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getIpHashPepper().getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(remoteAddress.trim().toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash client address", ex);
        }
    }

    public String referrerHost(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            String host = uri.getHost();
            return host == null || host.isBlank() ? null : truncate(host.toLowerCase(java.util.Locale.ROOT), 255);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String userAgentCategory(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        String normalized = userAgent.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("bot") || normalized.contains("crawler") || normalized.contains("spider")) {
            return "bot";
        }
        if (normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")) {
            return "mobile";
        }
        return "desktop";
    }

    public String correlationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            return null;
        }
        return truncate(correlationId.replaceAll("[^A-Za-z0-9._-]", ""), 128);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
