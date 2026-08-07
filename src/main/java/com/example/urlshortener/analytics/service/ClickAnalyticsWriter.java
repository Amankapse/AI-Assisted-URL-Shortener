package com.example.urlshortener.analytics.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ClickAnalyticsWriter {
    private final JdbcTemplate jdbcTemplate;

    public ClickAnalyticsWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public int persistBatch(List<ClickAnalyticsEvent> events) {
        Map<UUID, Integer> insertedByUrl = new HashMap<>();
        for (ClickAnalyticsEvent event : events) {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO click_events (id, url_id, clicked_at, ip_hash, user_agent, referer, correlation_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                    """,
                    event.eventId(),
                    event.urlId(),
                    Timestamp.valueOf(event.clickedAt()),
                    event.ipHash(),
                    event.userAgentCategory(),
                    event.referrerHost(),
                    event.correlationId()
            );
            if (inserted == 1) {
                insertedByUrl.merge(event.urlId(), 1, Integer::sum);
            }
        }
        insertedByUrl.forEach((urlId, delta) -> jdbcTemplate.update(
                "UPDATE short_urls SET click_count = click_count + ? WHERE id = ?",
                delta,
                urlId
        ));
        return insertedByUrl.values().stream().mapToInt(Integer::intValue).sum();
    }
}
