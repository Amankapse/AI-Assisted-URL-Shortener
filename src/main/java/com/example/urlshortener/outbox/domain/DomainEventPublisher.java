package com.example.urlshortener.outbox.domain;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
