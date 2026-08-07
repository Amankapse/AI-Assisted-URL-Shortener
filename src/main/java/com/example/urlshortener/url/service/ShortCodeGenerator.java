package com.example.urlshortener.url.service;

import com.example.urlshortener.url.config.ShortCodeProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ShortCodeProperties properties;

    public ShortCodeGenerator(ShortCodeProperties properties) {
        this.properties = properties;
    }

    public String generate() {
        int length = properties.getLength();
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
