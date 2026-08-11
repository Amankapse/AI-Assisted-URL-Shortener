package com.example.urlshortener.campaign.controller;

import com.example.urlshortener.campaign.dto.CampaignCreateRequest;
import com.example.urlshortener.campaign.dto.CampaignResponse;
import com.example.urlshortener.campaign.dto.CampaignUpdateRequest;
import com.example.urlshortener.campaign.service.CampaignService;
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

@Tag(name = "Campaigns")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/campaigns")
public class CampaignController {
    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Operation(summary = "Create campaign")
    @PostMapping
    public ResponseEntity<CampaignResponse> create(@PathVariable("workspaceId") UUID workspaceId, @Valid @RequestBody CampaignCreateRequest request) {
        CampaignResponse response = campaignService.create(workspaceId, request);
        return withEtag(ResponseEntity.status(HttpStatus.CREATED), response).body(response);
    }

    @Operation(summary = "List campaigns")
    @GetMapping
    public List<CampaignResponse> list(@PathVariable("workspaceId") UUID workspaceId) {
        return campaignService.list(workspaceId);
    }

    @Operation(summary = "Get campaign")
    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> get(@PathVariable("workspaceId") UUID workspaceId, @PathVariable("campaignId") UUID campaignId) {
        CampaignResponse response = campaignService.get(workspaceId, campaignId);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    @Operation(summary = "Update campaign")
    @PatchMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> update(@PathVariable("workspaceId") UUID workspaceId, @PathVariable("campaignId") UUID campaignId, @Valid @RequestBody CampaignUpdateRequest request) {
        CampaignResponse response = campaignService.update(workspaceId, campaignId, request);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    @Operation(summary = "Delete campaign")
    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> delete(@PathVariable("workspaceId") UUID workspaceId, @PathVariable("campaignId") UUID campaignId) {
        campaignService.delete(workspaceId, campaignId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity.BodyBuilder withEtag(ResponseEntity.BodyBuilder builder, CampaignResponse response) {
        return builder.eTag(UrlEtags.fromVersion(response.getVersion()));
    }
}
