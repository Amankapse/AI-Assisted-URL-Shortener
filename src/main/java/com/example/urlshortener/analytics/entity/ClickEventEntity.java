package com.example.urlshortener.analytics.entity;

import com.example.urlshortener.url.entity.ShortUrlEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_events_url_id", columnList = "url_id"),
        @Index(name = "idx_click_events_clicked_at", columnList = "clicked_at")
})
public class ClickEventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false, foreignKey = @ForeignKey(name = "fk_click_events_url"))
    private ShortUrlEntity url;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(length = 1024)
    private String referer;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    public ClickEventEntity() {
    }

    public ClickEventEntity(UUID id, ShortUrlEntity url, LocalDateTime clickedAt, String ipHash, String userAgent, String referer) {
        this.id = id;
        this.url = url;
        this.clickedAt = clickedAt;
        this.ipHash = ipHash;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    public ClickEventEntity(UUID id, ShortUrlEntity url, LocalDateTime clickedAt, String ipHash, String userAgent, String referer, String correlationId) {
        this(id, url, clickedAt, ipHash, userAgent, referer);
        this.correlationId = correlationId;
    }

    @PrePersist
    public void prePersist() {
        if (this.clickedAt == null) {
            this.clickedAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ShortUrlEntity getUrl() {
        return url;
    }

    public void setUrl(ShortUrlEntity url) {
        this.url = url;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(LocalDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
