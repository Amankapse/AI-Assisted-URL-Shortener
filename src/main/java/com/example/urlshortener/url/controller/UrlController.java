package com.example.urlshortener.url.controller;

import com.example.urlshortener.idempotency.service.IdempotencyService;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.dto.UpdateDestinationRequest;
import com.example.urlshortener.url.dto.UpdateShortUrlRequest;
import com.example.urlshortener.url.service.UrlService;
import com.example.urlshortener.url.web.UrlEtags;
import com.example.urlshortener.workspace.service.WorkspaceContext;
import com.example.urlshortener.workspace.service.WorkspaceContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

@Tag(name = "URL Management")
@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class UrlController {

    private final UrlService urlService;
    private final IdempotencyService idempotencyService;
    private final WorkspaceContextResolver workspaceContextResolver;

    public UrlController(UrlService urlService, IdempotencyService idempotencyService, WorkspaceContextResolver workspaceContextResolver) {
        this.urlService = urlService;
        this.idempotencyService = idempotencyService;
        this.workspaceContextResolver = workspaceContextResolver;
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
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(name = WorkspaceContextResolver.WORKSPACE_HEADER, required = false) String workspaceHeader) {
        return ResponseEntity.ok(urlService.list(page, size, workspaceHeader));
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
