package com.example.urlshortener.url.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int MIN_LENGTH = 7;
    private static final int MAX_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        int length = RANDOM.nextInt(MAX_LENGTH - MIN_LENGTH + 1) + MIN_LENGTH;
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
