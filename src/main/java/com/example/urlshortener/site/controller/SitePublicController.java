package com.example.urlshortener.site.controller;

import com.example.urlshortener.site.dto.SiteDtos.AnnouncementResponse;
import com.example.urlshortener.site.dto.SiteDtos.PublicContentPageResponse;
import com.example.urlshortener.site.dto.SiteDtos.SiteSettingsResponse;
import com.example.urlshortener.site.entity.AnnouncementAudience;
import com.example.urlshortener.site.entity.ContentPageKey;
import com.example.urlshortener.site.service.SiteExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Site Experience")
@RestController
@RequestMapping("/api/v1/site")
public class SitePublicController {
    private final SiteExperienceService siteExperienceService;

    public SitePublicController(SiteExperienceService siteExperienceService) {
        this.siteExperienceService = siteExperienceService;
    }

    @Operation(summary = "Get public site settings")
    @GetMapping("/settings")
    public SiteSettingsResponse settings() {
        return siteExperienceService.publicSettings();
    }

    @Operation(summary = "Get a published public content page")
    @GetMapping("/pages/{pageKey}")
    public PublicContentPageResponse page(@PathVariable("pageKey") ContentPageKey pageKey) {
        return siteExperienceService.publicPage(pageKey);
    }

    @Operation(summary = "List active announcements")
    @GetMapping("/announcements")
    public List<AnnouncementResponse> announcements(@RequestParam(name = "audience", required = false) AnnouncementAudience audience) {
        return siteExperienceService.publicAnnouncements(audience);
    }
}
