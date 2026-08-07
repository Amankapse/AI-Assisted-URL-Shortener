package com.example.urlshortener.url.controller;

import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.dto.UpdateShortUrlRequest;
import com.example.urlshortener.url.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @Operation(summary = "Create a new short URL")
    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = urlService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a short URL by id")
    @GetMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(urlService.get(id));
    }

    @Operation(summary = "List short URLs")
    @GetMapping
    public ResponseEntity<Page<ShortUrlResponse>> list(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(urlService.list(page, size));
    }

    @Operation(summary = "Update URL expiration timestamp")
    @PatchMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> updateExpiration(@PathVariable("id") UUID id, @Valid @RequestBody UpdateShortUrlRequest request) {
        return ResponseEntity.ok(urlService.updateExpiration(id, request));
    }

    @Operation(summary = "Disable a short URL")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        urlService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Disable a short URL")
    @PostMapping("/{id}/disable")
    public ResponseEntity<ShortUrlResponse> disable(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(urlService.disable(id));
    }

    @Operation(summary = "Enable a short URL")
    @PostMapping("/{id}/enable")
    public ResponseEntity<ShortUrlResponse> enable(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(urlService.enable(id));
    }
}
