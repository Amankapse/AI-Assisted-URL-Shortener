package com.example.urlshortener.url.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.url-organization")
public class UrlOrganizationProperties {
    private int tagMaxLength = 50;
    private int tagsPerUrl = 10;
    private int campaignNameMaxLength = 120;
    private int campaignDescriptionMaxLength = 500;
    private int searchQueryMaxLength = 100;
    private int defaultPageSize = 20;
    private int maxPageSize = 100;

    public int getTagMaxLength() {
        return tagMaxLength;
    }

    public void setTagMaxLength(int tagMaxLength) {
        this.tagMaxLength = tagMaxLength;
    }

    public int getTagsPerUrl() {
        return tagsPerUrl;
    }

    public void setTagsPerUrl(int tagsPerUrl) {
        this.tagsPerUrl = tagsPerUrl;
    }

    public int getCampaignNameMaxLength() {
        return campaignNameMaxLength;
    }

    public void setCampaignNameMaxLength(int campaignNameMaxLength) {
        this.campaignNameMaxLength = campaignNameMaxLength;
    }

    public int getCampaignDescriptionMaxLength() {
        return campaignDescriptionMaxLength;
    }

    public void setCampaignDescriptionMaxLength(int campaignDescriptionMaxLength) {
        this.campaignDescriptionMaxLength = campaignDescriptionMaxLength;
    }

    public int getSearchQueryMaxLength() {
        return searchQueryMaxLength;
    }

    public void setSearchQueryMaxLength(int searchQueryMaxLength) {
        this.searchQueryMaxLength = searchQueryMaxLength;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}
