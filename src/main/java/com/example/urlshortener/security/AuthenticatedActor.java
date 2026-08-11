package com.example.urlshortener.security;

import com.example.urlshortener.apikey.entity.ApiKeyScope;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.user.entity.UserRole;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedActor(
        AuditActorType actorType,
        UUID actorId,
        UUID workspaceId,
        UserRole userRole,
        Set<ApiKeyScope> apiKeyScopes
) {
    public boolean isApiKey() {
        return actorType == AuditActorType.API_KEY;
    }

    public boolean hasScope(ApiKeyScope scope) {
        return apiKeyScopes != null && apiKeyScopes.contains(scope);
    }
}
