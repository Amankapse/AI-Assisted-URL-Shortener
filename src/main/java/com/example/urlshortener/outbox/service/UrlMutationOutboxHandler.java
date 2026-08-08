package com.example.urlshortener.outbox.service;

import com.example.urlshortener.outbox.domain.OutboxEventHandler;
import com.example.urlshortener.outbox.domain.OutboxEventRecord;
import com.example.urlshortener.outbox.domain.OutboxEventType;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class UrlMutationOutboxHandler implements OutboxEventHandler {
    private static final Set<OutboxEventType> SUPPORTED = EnumSet.of(
            OutboxEventType.URL_CREATED,
            OutboxEventType.URL_DESTINATION_CHANGED,
            OutboxEventType.URL_EXPIRATION_CHANGED,
            OutboxEventType.URL_ENABLED,
            OutboxEventType.URL_DISABLED,
            OutboxEventType.URL_DELETED,
            OutboxEventType.URL_BLOCKED,
            OutboxEventType.URL_UNBLOCKED
    );

    @Override
    public String handlerName() {
        return "url-mutation";
    }

    @Override
    public boolean supports(OutboxEventType eventType, int eventVersion) {
        return eventVersion == 1 && SUPPORTED.contains(eventType);
    }

    @Override
    public void handle(OutboxEventRecord event) {
        // Stage 7 persists durable control-plane events. A future broker adapter can replace this local sink.
    }
}
