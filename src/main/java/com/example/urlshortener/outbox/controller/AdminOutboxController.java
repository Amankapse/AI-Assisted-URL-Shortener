package com.example.urlshortener.outbox.controller;

import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.outbox.domain.OutboxEventType;
import com.example.urlshortener.outbox.domain.OutboxStatus;
import com.example.urlshortener.outbox.dto.OutboxDtos.OutboxEventSummaryResponse;
import com.example.urlshortener.outbox.dto.OutboxDtos.OutboxPageResponse;
import com.example.urlshortener.outbox.service.OutboxEventStore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/outbox")
public class AdminOutboxController {
    private static final int MAX_PAGE_SIZE = 100;

    private final OutboxEventStore store;

    public AdminOutboxController(OutboxEventStore store) {
        this.store = store;
    }

    @GetMapping
    public OutboxPageResponse list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Invalid outbox pagination parameters");
        }
        String normalizedStatus = normalizeStatus(status);
        String normalizedEventType = normalizeEventType(eventType);
        return new OutboxPageResponse(
                store.findForAdmin(normalizedStatus, normalizedEventType, from, to, page, size).stream()
                        .map(OutboxEventSummaryResponse::from)
                        .toList(),
                page,
                size
        );
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OutboxStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported outbox status");
        }
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return null;
        }
        try {
            return OutboxEventType.valueOf(eventType.trim().toUpperCase(java.util.Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported outbox event type");
        }
    }
}
