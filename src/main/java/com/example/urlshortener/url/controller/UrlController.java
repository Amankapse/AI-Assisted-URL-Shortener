package com.example.urlshortener.url.controller;

import com.example.urlshortener.idempotency.service.IdempotencyService;
import com.example.urlshortener.tag.service.TagService;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.dto.UpdateDestinationRequest;
import com.example.urlshortener.url.dto.UpdateShortUrlRequest;
import com.example.urlshortener.url.dto.UpdateUrlCampaignRequest;
import com.example.urlshortener.url.dto.UpdateUrlTagsRequest;
import com.example.urlshortener.url.search.UrlSearchCriteria;
import com.example.urlshortener.url.search.UrlSearchService;
import com.example.urlshortener.url.search.UrlState;
import com.example.urlshortener.url.service.UrlService;
import com.example.urlshortener.url.web.UrlEtags;
import com.example.urlshortener.workspace.service.WorkspaceContext;
import com.example.urlshortener.workspace.service.WorkspaceContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.time.LocalDateTime;

@Tag(name = "URL Management")
@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class UrlController {

    private final UrlService urlService;
    private final UrlSearchService urlSearchService;
    private final IdempotencyService idempotencyService;
    private final WorkspaceContextResolver workspaceContextResolver;
    private final TagService tagService;

    public UrlController(UrlService urlService,
                         UrlSearchService urlSearchService,
                         IdempotencyService idempotencyService,
                         WorkspaceContextResolver workspaceContextResolver,
                         TagService tagService) {
        this.urlService = urlService;
        this.urlSearchService = urlSearchService;
        this.idempotencyService = idempotencyService;
        this.workspaceContextResolver = workspaceContextResolver;
        this.tagService = tagService;
    }

    @Operation(summary = "Create a new short URL")
    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader,
            @Valid @RequestBody CreateShortUrlRequest request) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        String actorScope = workspace.actorType().name().equals("API_KEY")
                ? "actor:api-key:" + workspace.actorId()
                : "actor:user:" + workspace.actorId();
        String scope = "workspace:" + workspace.workspaceId() + ":" + actorScope + ":operation:url-create";
        request.setTags(tagService.normalize(request.getTags()));
        ShortUrlResponse response = idempotencyService.execute(idempotencyKey, scope, request, () -> urlService.create(request, workspace), ShortUrlResponse.class);
        return withEtag(ResponseEntity.status(HttpStatus.CREATED), response).body(response);
    }

    @Operation(summary = "Get a short URL by id")
    @GetMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> get(@PathVariable("id") UUID id,
                                                @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader) {
        ShortUrlResponse response = urlService.get(id, workspaceHeader);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    @Operation(summary = "List short URLs")
    @GetMapping
    public ResponseEntity<Page<ShortUrlResponse>> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "state", required = false) UrlState state,
            @RequestParam(name = "campaignId", required = false) UUID campaignId,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "createdFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(name = "createdTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(name = "expiresBefore", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresBefore,
            @RequestParam(name = "expiresAfter", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAfter,
            @RequestParam(name = "customAlias", required = false) Boolean customAlias,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader) {
        return ResponseEntity.ok(urlSearchService.search(new UrlSearchCriteria(q, state, campaignId, tag, createdFrom, createdTo, expiresBefore, expiresAfter, customAlias, sort, page, size), workspaceHeader));
    }

    @Operation(summary = "Update URL expiration timestamp")
    @PatchMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> updateExpiration(@PathVariable("id") UUID id,
                                                             @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader,
                                                             @Valid @RequestBody UpdateShortUrlRequest request) {
        ShortUrlResponse response = urlService.updateExpiration(id, request, workspaceHeader);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    @Operation(summary = "Update URL destination")
    @PatchMapping("/{id}/destination")
    public ResponseEntity<ShortUrlResponse> updateDestination(
            @PathVariable("id") UUID id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader,
            @Valid @RequestBody UpdateDestinationRequest request) {
        ShortUrlResponse response = urlService.updateDestination(id, request, UrlEtags.requireVersion(ifMatch), workspaceHeader);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    @Operation(summary = "Assign or clear URL campaign")
    @PatchMapping("/{id}/campaign")
    public ResponseEntity<ShortUrlResponse> updateCampaign(
            @PathVariable("id") UUID id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader,
            @Valid @RequestBody UpdateUrlCampaignRequest request) {
        ShortUrlResponse response = urlService.updateCampaign(id, request, UrlEtags.requireVersion(ifMatch), workspaceHeader);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    @Operation(summary = "Replace URL tags")
    @PutMapping("/{id}/tags")
    public ResponseEntity<ShortUrlResponse> replaceTags(
            @PathVariable("id") UUID id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader,
            @Valid @RequestBody UpdateUrlTagsRequest request) {
        request.setTags(tagService.normalize(request.getTags()));
        ShortUrlResponse response = urlService.replaceTags(id, request, UrlEtags.requireVersion(ifMatch), workspaceHeader);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    @Operation(summary = "Disable a short URL")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id,
                                       @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader) {
        urlService.delete(id, workspaceHeader);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Disable a short URL")
    @PostMapping("/{id}/disable")
    public ResponseEntity<ShortUrlResponse> disable(@PathVariable("id") UUID id,
                                                    @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader) {
        ShortUrlResponse response = urlService.disable(id, workspaceHeader);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    @Operation(summary = "Enable a short URL")
    @PostMapping("/{id}/enable")
    public ResponseEntity<ShortUrlResponse> enable(@PathVariable("id") UUID id,
                                                   @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader) {
        ShortUrlResponse response = urlService.enable(id, workspaceHeader);
        return withEtag(ResponseEntity.ok(), response).body(response);
    }

    private ResponseEntity.BodyBuilder withEtag(ResponseEntity.BodyBuilder builder, ShortUrlResponse response) {
        return builder.eTag(UrlEtags.fromVersion(response.getVersion()));
    }
}
