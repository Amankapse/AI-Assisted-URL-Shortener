package com.example.urlshortener.campaign.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

public class CampaignResponse {
    private UUID id;
    private UUID workspaceId;
    private String name;
    private String normalizedName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonIgnore
    private long version;

    public CampaignResponse() {
    }

    public CampaignResponse(UUID id, UUID workspaceId, String name, String normalizedName, String description, LocalDateTime createdAt, LocalDateTime updatedAt, long version) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
