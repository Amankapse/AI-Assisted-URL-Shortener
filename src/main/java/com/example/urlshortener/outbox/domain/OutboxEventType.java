package com.example.urlshortener.outbox.domain;

public enum OutboxEventType {
    URL_CREATED,
    URL_DESTINATION_CHANGED,
    URL_EXPIRATION_CHANGED,
    URL_ENABLED,
    URL_DISABLED,
    URL_DELETED,
    URL_BLOCKED,
    URL_UNBLOCKED,
    URL_CACHE_INVALIDATION_REQUIRED,
    CLICK_RECORDED
}
