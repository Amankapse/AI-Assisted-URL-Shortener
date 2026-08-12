package com.example.urlshortener.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "site_settings")
public class SiteSettingsEntity {
    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Id
    private UUID id;

    @Column(name = "brand_name", nullable = false, length = 80)
    private String brandName;

    @Column(nullable = false, length = 160)
    private String tagline;

    @Column(name = "support_email", length = 320)
    private String supportEmail;

    @Column(name = "support_url", length = 500)
    private String supportUrl;

    @Column(name = "contact_text", length = 1000)
    private String contactText;

    @Column(name = "logo_asset_id")
    private UUID logoAssetId;

    @Column(name = "logo_dark_asset_id")
    private UUID logoDarkAssetId;

    @Column(name = "favicon_asset_id")
    private UUID faviconAssetId;

    @Column(name = "login_background_asset_id")
    private UUID loginBackgroundAssetId;

    @Column(name = "landing_hero_asset_id")
    private UUID landingHeroAssetId;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", nullable = false, length = 7)
    private String secondaryColor;

    @Column(name = "accent_color", nullable = false, length = 7)
    private String accentColor;

    @Column(name = "footer_description", nullable = false, length = 500)
    private String footerDescription;

    @Column(name = "footer_copyright", nullable = false, length = 160)
    private String footerCopyright;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }
    public String getSupportUrl() { return supportUrl; }
    public void setSupportUrl(String supportUrl) { this.supportUrl = supportUrl; }
    public String getContactText() { return contactText; }
    public void setContactText(String contactText) { this.contactText = contactText; }
    public UUID getLogoAssetId() { return logoAssetId; }
    public void setLogoAssetId(UUID logoAssetId) { this.logoAssetId = logoAssetId; }
    public UUID getLogoDarkAssetId() { return logoDarkAssetId; }
    public void setLogoDarkAssetId(UUID logoDarkAssetId) { this.logoDarkAssetId = logoDarkAssetId; }
    public UUID getFaviconAssetId() { return faviconAssetId; }
    public void setFaviconAssetId(UUID faviconAssetId) { this.faviconAssetId = faviconAssetId; }
    public UUID getLoginBackgroundAssetId() { return loginBackgroundAssetId; }
    public void setLoginBackgroundAssetId(UUID loginBackgroundAssetId) { this.loginBackgroundAssetId = loginBackgroundAssetId; }
    public UUID getLandingHeroAssetId() { return landingHeroAssetId; }
    public void setLandingHeroAssetId(UUID landingHeroAssetId) { this.landingHeroAssetId = landingHeroAssetId; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }
    public String getFooterDescription() { return footerDescription; }
    public void setFooterDescription(String footerDescription) { this.footerDescription = footerDescription; }
    public String getFooterCopyright() { return footerCopyright; }
    public void setFooterCopyright(String footerCopyright) { this.footerCopyright = footerCopyright; }
    public long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
