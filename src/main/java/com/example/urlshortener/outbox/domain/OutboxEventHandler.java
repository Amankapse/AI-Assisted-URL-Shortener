package com.example.urlshortener.outbox.domain;

public interface OutboxEventHandler {
    String handlerName();

    boolean supports(OutboxEventType eventType, int eventVersion);

    void handle(OutboxEventRecord event);
}
