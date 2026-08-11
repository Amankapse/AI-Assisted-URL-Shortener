package com.example.urlshortener.campaign.service;

import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.url.config.UrlOrganizationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CampaignNameNormalizer {
    private final UrlOrganizationProperties properties;

    public CampaignNameNormalizer(UrlOrganizationProperties properties) {
        this.properties = properties;
    }

    public String displayName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("name is required");
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() > properties.getCampaignNameMaxLength()) {
            throw new BadRequestException("name may not exceed " + properties.getCampaignNameMaxLength() + " characters");
        }
        return trimmed;
    }

    public String normalize(String value) {
        return displayName(value).toLowerCase(Locale.ROOT);
    }

    public String description(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > properties.getCampaignDescriptionMaxLength()) {
            throw new BadRequestException("description may not exceed " + properties.getCampaignDescriptionMaxLength() + " characters");
        }
        return trimmed;
    }
}
