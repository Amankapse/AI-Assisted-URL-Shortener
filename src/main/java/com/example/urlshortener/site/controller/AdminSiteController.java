package com.example.urlshortener.site.controller;

import com.example.urlshortener.site.dto.SiteDtos.AdminContentPageResponse;
import com.example.urlshortener.site.dto.SiteDtos.AnnouncementRequest;
import com.example.urlshortener.site.dto.SiteDtos.AnnouncementResponse;
import com.example.urlshortener.site.dto.SiteDtos.ContentPageUpdateRequest;
import com.example.urlshortener.site.dto.SiteDtos.MediaAssetRequest;
import com.example.urlshortener.site.dto.SiteDtos.MediaAssetResponse;
import com.example.urlshortener.site.dto.SiteDtos.SiteSettingsResponse;
import com.example.urlshortener.site.dto.SiteDtos.SiteSettingsUpdateRequest;
import com.example.urlshortener.site.entity.ContentPageKey;
import com.example.urlshortener.site.service.SiteExperienceService;
import com.example.urlshortener.url.web.UrlEtags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin Site Experience")
@RestController
@RequestMapping("/api/v1/admin/site")
public class AdminSiteController {
    private final SiteExperienceService siteExperienceService;

    public AdminSiteController(SiteExperienceService siteExperienceService) {
        this.siteExperienceService = siteExperienceService;
    }

    @Operation(summary = "Get editable site settings")
    @GetMapping("/settings")
    public ResponseEntity<SiteSettingsResponse> settings() {
        SiteSettingsResponse response = siteExperienceService.publicSettings();
        return withEtag(ResponseEntity.ok(), response.version()).body(response);
    }

    @Operation(summary = "Update site settings")
    @PutMapping("/settings")
    public ResponseEntity<SiteSettingsResponse> updateSettings(
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody SiteSettingsUpdateRequest request) {
        SiteSettingsResponse response = siteExperienceService.updateSettings(request, UrlEtags.requireVersion(ifMatch));
        return withEtag(ResponseEntity.ok(), response.version()).body(response);
    }

    @Operation(summary = "List editable content pages")
    @GetMapping("/content")
    public List<AdminContentPageResponse> pages() {
        return siteExperienceService.adminPages();
    }

    @Operation(summary = "Update content page")
    @PutMapping("/content/{pageKey}")
    public ResponseEntity<AdminContentPageResponse> updatePage(
            @PathVariable("pageKey") ContentPageKey pageKey,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ContentPageUpdateRequest request) {
        AdminContentPageResponse response = siteExperienceService.updatePage(pageKey, request, UrlEtags.requireVersion(ifMatch));
        return withEtag(ResponseEntity.ok(), response.version()).body(response);
    }

    @Operation(summary = "List announcements")
    @GetMapping("/announcements")
    public List<AnnouncementResponse> announcements() {
        return siteExperienceService.adminAnnouncements();
    }

    @Operation(summary = "Create announcement")
    @PostMapping("/announcements")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = siteExperienceService.createAnnouncement(request);
        return withEtag(ResponseEntity.status(HttpStatus.CREATED), response.version()).body(response);
    }

    @Operation(summary = "Update announcement")
    @PutMapping("/announcements/{id}")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable("id") UUID id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = siteExperienceService.updateAnnouncement(id, request, UrlEtags.requireVersion(ifMatch));
        return withEtag(ResponseEntity.ok(), response.version()).body(response);
    }

    @Operation(summary = "List media assets")
    @GetMapping("/media")
    public List<MediaAssetResponse> media() {
        return siteExperienceService.adminMedia();
    }

    @Operation(summary = "Register media asset")
    @PostMapping("/media")
    public ResponseEntity<MediaAssetResponse> registerMedia(@Valid @RequestBody MediaAssetRequest request) {
        MediaAssetResponse response = siteExperienceService.registerMedia(request);
        return withEtag(ResponseEntity.status(HttpStatus.CREATED), response.version()).body(response);
    }

    @Operation(summary = "Update media asset metadata")
    @PutMapping("/media/{id}")
    public ResponseEntity<MediaAssetResponse> updateMedia(
            @PathVariable("id") UUID id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody MediaAssetRequest request) {
        MediaAssetResponse response = siteExperienceService.updateMedia(id, request, UrlEtags.requireVersion(ifMatch));
        return withEtag(ResponseEntity.ok(), response.version()).body(response);
    }

    private ResponseEntity.BodyBuilder withEtag(ResponseEntity.BodyBuilder builder, long version) {
        return builder.eTag(UrlEtags.fromVersion(version));
    }
}
