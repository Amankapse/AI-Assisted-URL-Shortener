package com.example.urlshortener.outbox.domain;

public class OutboxHandlingException extends RuntimeException {
    private final OutboxFailureType failureType;
    private final String errorCode;

    public OutboxHandlingException(OutboxFailureType failureType, String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.failureType = failureType;
        this.errorCode = errorCode;
    }

    public OutboxFailureType failureType() {
        return failureType;
    }

    public String errorCode() {
        return errorCode;
    }

    public static OutboxHandlingException transientFailure(String errorCode, Throwable cause) {
        return new OutboxHandlingException(OutboxFailureType.TRANSIENT, errorCode, cause);
    }

    public static OutboxHandlingException permanentFailure(String errorCode, Throwable cause) {
        return new OutboxHandlingException(OutboxFailureType.PERMANENT, errorCode, cause);
    }
}
