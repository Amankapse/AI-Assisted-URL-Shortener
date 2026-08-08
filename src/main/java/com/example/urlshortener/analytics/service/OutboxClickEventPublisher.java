package com.example.urlshortener.analytics.service;

import com.example.urlshortener.outbox.domain.DomainEvent;
import com.example.urlshortener.outbox.domain.DomainEventPublisher;
import com.example.urlshortener.outbox.domain.OutboxEventType;
import com.example.urlshortener.outbox.payload.ClickEventPayloadV1;
import com.example.urlshortener.redirect.service.RedirectTarget;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.analytics", name = "publisher", havingValue = "outbox")
public class OutboxClickEventPublisher implements ClickEventPublisher {
    private final ClickPrivacySanitizer sanitizer;
    private final DomainEventPublisher domainEventPublisher;
    private final AnalyticsCounters counters;
    private final Clock clock;

    public OutboxClickEventPublisher(ClickPrivacySanitizer sanitizer,
                                     DomainEventPublisher domainEventPublisher,
                                     AnalyticsCounters counters,
                                     Clock clock) {
        this.sanitizer = sanitizer;
        this.domainEventPublisher = domainEventPublisher;
        this.counters = counters;
        this.clock = clock;
    }

    @Override
    public void publish(RedirectTarget target, HttpServletRequest request) {
        UUID eventId = UUID.randomUUID();
        LocalDateTime clickedAt = LocalDateTime.now(clock);
        ClickEventPayloadV1 payload = new ClickEventPayloadV1(
                eventId,
                target.urlId(),
                clickedAt,
                sanitizer.ipHash(request),
                sanitizer.userAgentCategory(request),
                sanitizer.referrerHost(request),
                sanitizer.correlationId(request)
        );
        domainEventPublisher.publish(new DomainEvent(
                eventId,
                null,
                "SHORT_URL",
                target.urlId().toString(),
                OutboxEventType.CLICK_RECORDED,
                1,
                payload,
                clickedAt
        ));
        counters.accepted();
    }
}
