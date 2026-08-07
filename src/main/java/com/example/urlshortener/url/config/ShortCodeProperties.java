package com.example.urlshortener.url.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shortener.code")
public class ShortCodeProperties {
    private int length = 8;
    private int maxRetries = 5;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        if (length < 7 || length > 16) {
            throw new IllegalArgumentException("shortener.code.length must be between 7 and 16");
        }
        this.length = length;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        if (maxRetries < 1 || maxRetries > 100) {
            throw new IllegalArgumentException("shortener.code.max-retries must be between 1 and 100");
        }
        this.maxRetries = maxRetries;
    }
}
