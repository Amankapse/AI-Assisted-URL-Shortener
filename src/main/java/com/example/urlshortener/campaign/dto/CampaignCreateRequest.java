package com.example.urlshortener.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CampaignCreateRequest {
    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name may not exceed 120 characters")
    private String name;

    @Size(max = 500, message = "description may not exceed 500 characters")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
