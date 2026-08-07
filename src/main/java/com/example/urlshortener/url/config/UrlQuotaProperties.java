package com.example.urlshortener.url.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shortener.quota")
public class UrlQuotaProperties {
    private boolean enabled = true;
    private long dailyCreationsPerUser = 10_000;
    private long maxActiveLinksPerUser = 100_000;
    private long dailyCustomAliasesPerUser = 1_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDailyCreationsPerUser() {
        return dailyCreationsPerUser;
    }

    public void setDailyCreationsPerUser(long dailyCreationsPerUser) {
        this.dailyCreationsPerUser = requirePositive(dailyCreationsPerUser, "daily-creations-per-user");
    }

    public long getMaxActiveLinksPerUser() {
        return maxActiveLinksPerUser;
    }

    public void setMaxActiveLinksPerUser(long maxActiveLinksPerUser) {
        this.maxActiveLinksPerUser = requirePositive(maxActiveLinksPerUser, "max-active-links-per-user");
    }

    public long getDailyCustomAliasesPerUser() {
        return dailyCustomAliasesPerUser;
    }

    public void setDailyCustomAliasesPerUser(long dailyCustomAliasesPerUser) {
        this.dailyCustomAliasesPerUser = requirePositive(dailyCustomAliasesPerUser, "daily-custom-aliases-per-user");
    }

    private long requirePositive(long value, String property) {
        if (value < 1) {
            throw new IllegalArgumentException("shortener.quota." + property + " must be positive");
        }
        return value;
    }
}
