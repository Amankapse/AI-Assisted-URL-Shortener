package com.example.urlshortener.url.web;

import com.example.urlshortener.common.exception.PreconditionFailedException;
import com.example.urlshortener.common.exception.PreconditionRequiredException;

public final class UrlEtags {
    private UrlEtags() {
    }

    public static String fromVersion(long version) {
        return "\"" + version + "\"";
    }

    public static long requireVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionRequiredException("If-Match header is required for this mutation.");
        }
        String trimmed = ifMatch.trim();
        if ("*".equals(trimmed)) {
            throw new PreconditionFailedException("Wildcard If-Match is not supported for URL mutations.");
        }
        if (trimmed.startsWith("W/")) {
            throw new PreconditionFailedException("Weak ETags are not supported for URL mutations.");
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        try {
            long version = Long.parseLong(trimmed);
            if (version < 0) {
                throw new NumberFormatException("negative");
            }
            return version;
        } catch (NumberFormatException ex) {
            throw new PreconditionFailedException("If-Match header does not match the current URL version.");
        }
    }
}
