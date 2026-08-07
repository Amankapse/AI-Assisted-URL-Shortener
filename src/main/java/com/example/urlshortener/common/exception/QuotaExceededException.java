package com.example.urlshortener.common.exception;

public class QuotaExceededException extends RuntimeException {
    private final String code;

    public QuotaExceededException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
