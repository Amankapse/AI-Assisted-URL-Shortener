package com.example.urlshortener.campaign.dto;

import java.util.UUID;

public record CampaignSummaryResponse(UUID id, String name, String normalizedName) {
}
