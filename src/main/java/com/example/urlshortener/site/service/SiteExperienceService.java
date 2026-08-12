package com.example.urlshortener.site.service;

import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.service.AuditService;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.PreconditionFailedException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.site.dto.SiteDtos.AdminContentPageResponse;
import com.example.urlshortener.site.dto.SiteDtos.AnnouncementRequest;
import com.example.urlshortener.site.dto.SiteDtos.AnnouncementResponse;
import com.example.urlshortener.site.dto.SiteDtos.ContentPageUpdateRequest;
import com.example.urlshortener.site.dto.SiteDtos.MediaAssetRequest;
import com.example.urlshortener.site.dto.SiteDtos.MediaAssetResponse;
import com.example.urlshortener.site.dto.SiteDtos.PublicContentPageResponse;
import com.example.urlshortener.site.dto.SiteDtos.PublicMediaAssetResponse;
import com.example.urlshortener.site.dto.SiteDtos.SiteSettingsResponse;
import com.example.urlshortener.site.dto.SiteDtos.SiteSettingsUpdateRequest;
import com.example.urlshortener.site.entity.*;
import com.example.urlshortener.site.repository.AnnouncementRepository;
import com.example.urlshortener.site.repository.ContentPageRepository;
import com.example.urlshortener.site.repository.MediaAssetRepository;
import com.example.urlshortener.site.repository.SiteSettingsRepository;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.user.service.OwnerIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SiteExperienceService {
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final LocalDateTime ALWAYS_STARTED = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(2999, 12, 31, 23, 59);

    private final SiteSettingsRepository settingsRepository;
    private final ContentPageRepository contentPageRepository;
    private final AnnouncementRepository announcementRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final AuditService auditService;
    private final Clock clock;

    public SiteExperienceService(SiteSettingsRepository settingsRepository,
                                 ContentPageRepository contentPageRepository,
                                 AnnouncementRepository announcementRepository,
                                 MediaAssetRepository mediaAssetRepository,
                                 CurrentOwnerProvider currentOwnerProvider,
                                 AuditService auditService,
                                 Clock clock) {
        this.settingsRepository = settingsRepository;
        this.contentPageRepository = contentPageRepository;
        this.announcementRepository = announcementRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.currentOwnerProvider = currentOwnerProvider;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SiteSettingsResponse publicSettings() {
        return settingsRepository.findById(SiteSettingsEntity.SINGLETON_ID)
                .map(this::toSettingsResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Site settings not found"));
    }

    @Transactional
    public SiteSettingsResponse updateSettings(SiteSettingsUpdateRequest request, long expectedVersion) {
        OwnerIdentity actor = currentOwnerProvider.getCurrentOwner();
        SiteSettingsEntity entity = settingsRepository.findById(SiteSettingsEntity.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Site settings not found"));
        requireVersion(entity.getVersion(), expectedVersion);
        validateColor("primaryColor", request.primaryColor());
        validateColor("secondaryColor", request.secondaryColor());
        validateColor("accentColor", request.accentColor());
        validateContrast("primaryColor", request.primaryColor());
        validateContrast("accentColor", request.accentColor());
        validateHttpsOptional("supportUrl", request.supportUrl(), false);
        requireAssetIfSet(request.logoAssetId());
        requireAssetIfSet(request.logoDarkAssetId());
        requireAssetIfSet(request.faviconAssetId());
        requireAssetIfSet(request.loginBackgroundAssetId());
        requireAssetIfSet(request.landingHeroAssetId());

        entity.setBrandName(strip(request.brandName()));
        entity.setTagline(strip(request.tagline()));
        entity.setSupportEmail(stripNullable(request.supportEmail()));
        entity.setSupportUrl(stripNullable(request.supportUrl()));
        entity.setContactText(stripNullable(request.contactText()));
        entity.setLogoAssetId(request.logoAssetId());
        entity.setLogoDarkAssetId(request.logoDarkAssetId());
        entity.setFaviconAssetId(request.faviconAssetId());
        entity.setLoginBackgroundAssetId(request.loginBackgroundAssetId());
        entity.setLandingHeroAssetId(request.landingHeroAssetId());
        entity.setPrimaryColor(request.primaryColor());
        entity.setSecondaryColor(request.secondaryColor());
        entity.setAccentColor(request.accentColor());
        entity.setFooterDescription(strip(request.footerDescription()));
        entity.setFooterCopyright(strip(request.footerCopyright()));
        entity.setUpdatedBy(actor.userId());
        SiteSettingsEntity saved = settingsRepository.save(entity);
        auditService.recordUser(AuditAction.SITE_SETTINGS_UPDATED, null, actor.userId(), AuditResourceType.SITE_SETTINGS,
                saved.getId(), Map.of("changedFields", "settings", "version", saved.getVersion()));
        return toSettingsResponse(saved);
    }

    @Transactional(readOnly = true)
    public PublicContentPageResponse publicPage(ContentPageKey pageKey) {
        return contentPageRepository.findByPageKeyAndStatus(pageKey, ContentStatus.PUBLISHED)
                .map(this::toPublicPage)
                .orElseThrow(() -> new ResourceNotFoundException("Published content page not found"));
    }

    @Transactional(readOnly = true)
    public List<AdminContentPageResponse> adminPages() {
        return contentPageRepository.findAllByOrderByPageKeyAsc().stream().map(this::toAdminPage).toList();
    }

    @Transactional
    public AdminContentPageResponse updatePage(ContentPageKey pageKey, ContentPageUpdateRequest request, long expectedVersion) {
        OwnerIdentity actor = currentOwnerProvider.getCurrentOwner();
        ContentPageEntity entity = contentPageRepository.findByPageKey(pageKey)
                .orElseThrow(() -> new ResourceNotFoundException("Content page not found"));
        requireVersion(entity.getVersion(), expectedVersion);
        validatePlainText("content", request.content());
        entity.setTitle(strip(request.title()));
        entity.setSummary(strip(request.summary()));
        entity.setContent(strip(request.content()));
        entity.setStatus(request.status());
        if (request.status() == ContentStatus.PUBLISHED) {
            entity.setPublishedAt(LocalDateTime.now(clock));
        }
        entity.setUpdatedBy(actor.userId());
        ContentPageEntity saved = contentPageRepository.save(entity);
        auditService.recordUser(request.status() == ContentStatus.PUBLISHED ? AuditAction.CONTENT_PAGE_PUBLISHED : AuditAction.CONTENT_PAGE_UPDATED,
                null, actor.userId(), AuditResourceType.CONTENT_PAGE, saved.getId(),
                Map.of("pageKey", saved.getPageKey().name(), "version", saved.getVersion()));
        return toAdminPage(saved);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> publicAnnouncements(AnnouncementAudience audience) {
        List<AnnouncementAudience> audiences = switch (audience == null ? AnnouncementAudience.PUBLIC : audience) {
            case PUBLIC -> List.of(AnnouncementAudience.PUBLIC);
            case AUTHENTICATED -> List.of(AnnouncementAudience.PUBLIC, AnnouncementAudience.AUTHENTICATED);
            case ADMIN -> List.of(AnnouncementAudience.PUBLIC, AnnouncementAudience.AUTHENTICATED, AnnouncementAudience.ADMIN);
        };
        LocalDateTime now = LocalDateTime.now(clock);
        return announcementRepository.findByEnabledTrueAndAudienceInAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByCreatedAtDesc(audiences, now, now)
                .stream().map(this::toAnnouncement).toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> adminAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toAnnouncement).toList();
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        OwnerIdentity actor = currentOwnerProvider.getCurrentOwner();
        AnnouncementEntity entity = new AnnouncementEntity();
        entity.setId(UUID.randomUUID());
        applyAnnouncement(entity, request);
        entity.setCreatedBy(actor.userId());
        entity.setUpdatedBy(actor.userId());
        AnnouncementEntity saved = announcementRepository.save(entity);
        auditService.recordUser(AuditAction.ANNOUNCEMENT_CREATED, null, actor.userId(), AuditResourceType.ANNOUNCEMENT,
                saved.getId(), Map.of("announcementId", saved.getId().toString(), "severity", saved.getSeverity().name()));
        return toAnnouncement(saved);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(UUID id, AnnouncementRequest request, long expectedVersion) {
        OwnerIdentity actor = currentOwnerProvider.getCurrentOwner();
        AnnouncementEntity entity = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        requireVersion(entity.getVersion(), expectedVersion);
        boolean wasEnabled = entity.isEnabled();
        applyAnnouncement(entity, request);
        entity.setUpdatedBy(actor.userId());
        AnnouncementEntity saved = announcementRepository.save(entity);
        auditService.recordUser(!saved.isEnabled() && wasEnabled ? AuditAction.ANNOUNCEMENT_DISABLED : AuditAction.ANNOUNCEMENT_UPDATED,
                null, actor.userId(), AuditResourceType.ANNOUNCEMENT, saved.getId(),
                Map.of("announcementId", saved.getId().toString(), "enabled", saved.isEnabled(), "version", saved.getVersion()));
        return toAnnouncement(saved);
    }

    @Transactional(readOnly = true)
    public List<MediaAssetResponse> adminMedia() {
        return mediaAssetRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toMedia).toList();
    }

    @Transactional
    public MediaAssetResponse registerMedia(MediaAssetRequest request) {
        OwnerIdentity actor = currentOwnerProvider.getCurrentOwner();
        validateMedia(request);
        MediaAssetEntity entity = new MediaAssetEntity();
        entity.setId(UUID.randomUUID());
        applyMedia(entity, request);
        entity.setCreatedBy(actor.userId());
        MediaAssetEntity saved = mediaAssetRepository.save(entity);
        auditService.recordUser(AuditAction.MEDIA_ASSET_REGISTERED, null, actor.userId(), AuditResourceType.MEDIA_ASSET,
                saved.getId(), Map.of("mediaAssetId", saved.getId().toString(), "sourceName", saved.getSourceName() == null ? "" : saved.getSourceName()));
        return toMedia(saved);
    }

    @Transactional
    public MediaAssetResponse updateMedia(UUID id, MediaAssetRequest request, long expectedVersion) {
        OwnerIdentity actor = currentOwnerProvider.getCurrentOwner();
        validateMedia(request);
        MediaAssetEntity entity = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found"));
        requireVersion(entity.getVersion(), expectedVersion);
        applyMedia(entity, request);
        MediaAssetEntity saved = mediaAssetRepository.save(entity);
        auditService.recordUser(AuditAction.MEDIA_ASSET_UPDATED, null, actor.userId(), AuditResourceType.MEDIA_ASSET,
                saved.getId(), Map.of("mediaAssetId", saved.getId().toString(), "enabled", saved.isEnabled()));
        return toMedia(saved);
    }

    private void applyAnnouncement(AnnouncementEntity entity, AnnouncementRequest request) {
        if (request.endAt() != null && request.startAt() != null && request.endAt().isBefore(request.startAt())) {
            throw new BadRequestException("Announcement endAt must be after startAt");
        }
        validatePlainText("message", request.message());
        entity.setTitle(strip(request.title()));
        entity.setMessage(strip(request.message()));
        entity.setSeverity(request.severity());
        entity.setAudience(request.audience());
        entity.setEnabled(request.enabled());
        entity.setStartAt(request.startAt() == null ? ALWAYS_STARTED : request.startAt());
        entity.setEndAt(request.endAt() == null ? FAR_FUTURE : request.endAt());
        entity.setDismissible(request.dismissible());
    }

    private void validateMedia(MediaAssetRequest request) {
        validateHttpsOptional("url", request.url(), true);
        validateHttpsOptional("sourceUrl", request.sourceUrl(), false);
        if (request.url().toLowerCase(Locale.ROOT).contains(".svg")) {
            throw new BadRequestException("SVG media assets are not accepted in this stage");
        }
    }

    private void applyMedia(MediaAssetEntity entity, MediaAssetRequest request) {
        entity.setName(strip(request.name()));
        entity.setUrl(strip(request.url()));
        entity.setAltText(strip(request.altText()));
        entity.setSourceName(stripNullable(request.sourceName()));
        entity.setSourceUrl(stripNullable(request.sourceUrl()));
        entity.setLicense(stripNullable(request.license()));
        entity.setAttribution(stripNullable(request.attribution()));
        entity.setEnabled(request.enabled());
    }

    private SiteSettingsResponse toSettingsResponse(SiteSettingsEntity entity) {
        return new SiteSettingsResponse(entity.getBrandName(), entity.getTagline(), entity.getSupportEmail(), entity.getSupportUrl(),
                entity.getContactText(), publicAsset(entity.getLogoAssetId()), publicAsset(entity.getLogoDarkAssetId()),
                publicAsset(entity.getFaviconAssetId()), publicAsset(entity.getLoginBackgroundAssetId()), publicAsset(entity.getLandingHeroAssetId()),
                entity.getPrimaryColor(), entity.getSecondaryColor(), entity.getAccentColor(), entity.getFooterDescription(),
                entity.getFooterCopyright(), entity.getVersion());
    }

    private PublicMediaAssetResponse publicAsset(UUID id) {
        if (id == null) {
            return null;
        }
        return mediaAssetRepository.findById(id)
                .filter(MediaAssetEntity::isEnabled)
                .map(asset -> new PublicMediaAssetResponse(asset.getId(), asset.getUrl(), asset.getAltText()))
                .orElse(null);
    }

    private PublicContentPageResponse toPublicPage(ContentPageEntity entity) {
        return new PublicContentPageResponse(entity.getPageKey(), entity.getTitle(), entity.getSummary(), entity.getContent(), entity.getPublishedAt());
    }

    private AdminContentPageResponse toAdminPage(ContentPageEntity entity) {
        return new AdminContentPageResponse(entity.getId(), entity.getPageKey(), entity.getTitle(), entity.getSummary(), entity.getContent(),
                entity.getStatus(), entity.getVersion(), entity.getUpdatedAt(), entity.getPublishedAt());
    }

    private AnnouncementResponse toAnnouncement(AnnouncementEntity entity) {
        return new AnnouncementResponse(entity.getId(), entity.getTitle(), entity.getMessage(), entity.getSeverity(), entity.getAudience(),
                entity.isEnabled(), entity.getStartAt(), entity.getEndAt(), entity.isDismissible(), entity.getVersion());
    }

    private MediaAssetResponse toMedia(MediaAssetEntity entity) {
        return new MediaAssetResponse(entity.getId(), entity.getName(), entity.getUrl(), entity.getAltText(), entity.getSourceName(),
                entity.getSourceUrl(), entity.getLicense(), entity.getAttribution(), entity.isEnabled(), entity.getVersion());
    }

    private void requireVersion(long actual, long expected) {
        if (actual != expected) {
            throw new PreconditionFailedException("If-Match header does not match the current content version.");
        }
    }

    private void validateColor(String name, String value) {
        if (value == null || !HEX_COLOR.matcher(value).matches()) {
            throw new BadRequestException(name + " must use #RRGGBB format");
        }
    }

    private void validateContrast(String name, String backgroundColor) {
        if (contrast(backgroundColor, "#ffffff") < 4.5) {
            throw new BadRequestException(name + " does not provide sufficient contrast with white foreground text");
        }
    }

    private double contrast(String one, String two) {
        double a = luminance(one);
        double b = luminance(two);
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double luminance(String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
    }

    private double channel(int value) {
        double normalized = value / 255.0;
        return normalized <= 0.03928 ? normalized / 12.92 : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }

    private void validateHttpsOptional(String field, String value, boolean required) {
        String stripped = stripNullable(value);
        if (stripped == null) {
            if (required) {
                throw new BadRequestException(field + " is required");
            }
            return;
        }
        URI uri;
        try {
            uri = URI.create(stripped);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(field + " must be a valid HTTPS URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new BadRequestException(field + " must be a valid HTTPS URL");
        }
    }

    private void requireAssetIfSet(UUID id) {
        if (id != null && mediaAssetRepository.findById(id).filter(MediaAssetEntity::isEnabled).isEmpty()) {
            throw new BadRequestException("Referenced media asset must exist and be enabled");
        }
    }

    private void validatePlainText(String field, String value) {
        if (value != null && (value.contains("<script") || value.contains("</") || value.contains("javascript:"))) {
            throw new BadRequestException(field + " must be plain text");
        }
    }

    private String strip(String value) {
        return value == null ? "" : value.strip();
    }

    private String stripNullable(String value) {
        String stripped = value == null ? null : value.strip();
        return stripped == null || stripped.isBlank() ? null : stripped;
    }
}
