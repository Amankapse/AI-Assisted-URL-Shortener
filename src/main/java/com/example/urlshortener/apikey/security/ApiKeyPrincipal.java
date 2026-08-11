package com.example.urlshortener.apikey.security;

import com.example.urlshortener.apikey.entity.ApiKeyScope;

import java.util.Set;
import java.util.UUID;

public record ApiKeyPrincipal(UUID apiKeyId, UUID workspaceId, Set<ApiKeyScope> scopes) {
    public boolean hasScope(ApiKeyScope scope) {
        return scopes.contains(scope);
    }
}
