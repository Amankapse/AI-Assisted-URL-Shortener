package com.example.urlshortener.apikey.controller;

import com.example.urlshortener.apikey.dto.ApiKeyDtos.ApiKeyCreatedResponse;
import com.example.urlshortener.apikey.dto.ApiKeyDtos.ApiKeyResponse;
import com.example.urlshortener.apikey.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.example.urlshortener.apikey.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/api-keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> create(@PathVariable("workspaceId") UUID workspaceId,
                                                        @Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.create(workspaceId, request));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> list(@PathVariable("workspaceId") UUID workspaceId) {
        return ResponseEntity.ok(apiKeyService.list(workspaceId));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiKeyResponse> revoke(@PathVariable("workspaceId") UUID workspaceId,
                                                 @PathVariable("id") UUID id) {
        return ResponseEntity.ok(apiKeyService.revoke(workspaceId, id));
    }
}
