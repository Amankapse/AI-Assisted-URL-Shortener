package com.example.urlshortener.admin.controller;

import com.example.urlshortener.url.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin URL Moderation")
@RestController
@RequestMapping("/api/v1/admin/urls")
public class AdminUrlController {
    private final UrlService urlService;

    public AdminUrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @Operation(summary = "Block a short URL")
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> block(@PathVariable("id") UUID id) {
        urlService.block(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unblock a short URL")
    @PostMapping("/{id}/unblock")
    public ResponseEntity<Void> unblock(@PathVariable("id") UUID id) {
        urlService.unblock(id);
        return ResponseEntity.noContent().build();
    }
}
