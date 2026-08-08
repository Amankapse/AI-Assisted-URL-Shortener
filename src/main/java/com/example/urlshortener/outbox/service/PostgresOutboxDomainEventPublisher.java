package com.example.urlshortener.outbox.service;

import com.example.urlshortener.outbox.domain.DomainEvent;
import com.example.urlshortener.outbox.domain.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostgresOutboxDomainEventPublisher implements DomainEventPublisher {
    private final OutboxEventStore store;

    public PostgresOutboxDomainEventPublisher(OutboxEventStore store) {
        this.store = store;
    }

    @Override
    @Transactional
    public void publish(DomainEvent event) {
        store.insert(event);
    }
}
