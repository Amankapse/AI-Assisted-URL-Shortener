package com.example.urlshortener.outbox.service;

import com.example.urlshortener.analytics.service.ClickAnalyticsEvent;
import com.example.urlshortener.analytics.service.ClickAnalyticsWriter;
import com.example.urlshortener.outbox.domain.OutboxEventHandler;
import com.example.urlshortener.outbox.domain.OutboxEventRecord;
import com.example.urlshortener.outbox.domain.OutboxEventType;
import com.example.urlshortener.outbox.domain.OutboxHandlingException;
import com.example.urlshortener.outbox.payload.ClickEventPayloadV1;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalyticsOutboxHandler implements OutboxEventHandler {
    private final ClickAnalyticsWriter writer;
    private final ObjectMapper objectMapper;

    public AnalyticsOutboxHandler(ClickAnalyticsWriter writer, ObjectMapper objectMapper) {
        this.writer = writer;
        this.objectMapper = objectMapper;
    }

    @Override
    public String handlerName() {
        return "analytics";
    }

    @Override
    public boolean supports(OutboxEventType eventType, int eventVersion) {
        return eventType == OutboxEventType.CLICK_RECORDED && eventVersion == 1;
    }

    @Override
    public void handle(OutboxEventRecord event) {
        try {
            ClickEventPayloadV1 payload = objectMapper.readValue(event.payload(), ClickEventPayloadV1.class);
            writer.persistBatch(List.of(new ClickAnalyticsEvent(
                    payload.eventId(),
                    payload.urlId(),
                    payload.clickedAt(),
                    payload.ipHash(),
                    payload.userAgentCategory(),
                    payload.referrerHost(),
                    payload.correlationId()
            )));
        } catch (TransientDataAccessException ex) {
            throw OutboxHandlingException.transientFailure("database_transient", ex);
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException ex) {
            throw OutboxHandlingException.permanentFailure("malformed_payload", ex);
        }
    }
}
