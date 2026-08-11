package com.example.urlshortener.security;

import com.example.urlshortener.apikey.security.ApiKeyPrincipal;
import com.example.urlshortener.audit.entity.AuditActorType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AuthenticatedActorProvider {
    public AuthenticatedActor getRequiredActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Authenticated actor is required");
        }
        if (authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return new AuthenticatedActor(AuditActorType.USER, principal.userId(), null, principal.role(), Set.of());
        }
        if (authentication.getPrincipal() instanceof ApiKeyPrincipal principal) {
            return new AuthenticatedActor(AuditActorType.API_KEY, principal.apiKeyId(), principal.workspaceId(), null, principal.scopes());
        }
        throw new IllegalStateException("Unsupported authenticated actor");
    }
}
