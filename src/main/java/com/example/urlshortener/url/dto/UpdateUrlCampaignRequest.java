package com.example.urlshortener.url.dto;

import java.util.UUID;

public class UpdateUrlCampaignRequest {
    private UUID campaignId;

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }
}
