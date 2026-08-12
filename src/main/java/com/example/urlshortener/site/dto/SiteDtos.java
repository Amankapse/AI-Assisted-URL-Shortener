package com.example.urlshortener.site.dto;

import com.example.urlshortener.site.entity.AnnouncementAudience;
import com.example.urlshortener.site.entity.AnnouncementSeverity;
import com.example.urlshortener.site.entity.ContentPageKey;
import com.example.urlshortener.site.entity.ContentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class SiteDtos {
    private SiteDtos() {
    }

    public record PublicMediaAssetResponse(
            UUID id,
            String url,
            String altText
    ) {
    }

    public record SiteSettingsResponse(
            String brandName,
            String tagline,
            String supportEmail,
            String supportUrl,
            String contactText,
            PublicMediaAssetResponse logo,
            PublicMediaAssetResponse logoDark,
            PublicMediaAssetResponse favicon,
            PublicMediaAssetResponse loginBackground,
            PublicMediaAssetResponse landingHero,
            String primaryColor,
            String secondaryColor,
            String accentColor,
            String footerDescription,
            String footerCopyright,
            long version
    ) {
    }

    public record SiteSettingsUpdateRequest(
            @NotBlank @Size(max = 80) String brandName,
            @NotBlank @Size(max = 160) String tagline,
            @Email @Size(max = 320) String supportEmail,
            @Size(max = 500) String supportUrl,
            @Size(max = 1000) String contactText,
            UUID logoAssetId,
            UUID logoDarkAssetId,
            UUID faviconAssetId,
            UUID loginBackgroundAssetId,
            UUID landingHeroAssetId,
            @NotBlank @Size(min = 7, max = 7) String primaryColor,
            @NotBlank @Size(min = 7, max = 7) String secondaryColor,
            @NotBlank @Size(min = 7, max = 7) String accentColor,
            @NotBlank @Size(max = 500) String footerDescription,
            @NotBlank @Size(max = 160) String footerCopyright
    ) {
    }

    public record PublicContentPageResponse(
            ContentPageKey pageKey,
            String title,
            String summary,
            String content,
            LocalDateTime publishedAt
    ) {
    }

    public record AdminContentPageResponse(
            UUID id,
            ContentPageKey pageKey,
            String title,
            String summary,
            String content,
            ContentStatus status,
            long version,
            LocalDateTime updatedAt,
            LocalDateTime publishedAt
    ) {
    }

    public record ContentPageUpdateRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 500) String summary,
            @NotBlank @Size(max = 8000) String content,
            @NotNull ContentStatus status
    ) {
    }

    public record AnnouncementResponse(
            UUID id,
            String title,
            String message,
            AnnouncementSeverity severity,
            AnnouncementAudience audience,
            boolean enabled,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean dismissible,
            long version
    ) {
    }

    public record AnnouncementRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 1000) String message,
            @NotNull AnnouncementSeverity severity,
            @NotNull AnnouncementAudience audience,
            boolean enabled,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean dismissible
    ) {
    }

    public record MediaAssetResponse(
            UUID id,
            String name,
            String url,
            String altText,
            String sourceName,
            String sourceUrl,
            String license,
            String attribution,
            boolean enabled,
            long version
    ) {
    }

    public record MediaAssetRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 800) String url,
            @NotBlank @Size(max = 200) String altText,
            @Size(max = 120) String sourceName,
            @Size(max = 800) String sourceUrl,
            @Size(max = 160) String license,
            @Size(max = 500) String attribution,
            boolean enabled
    ) {
    }
}
