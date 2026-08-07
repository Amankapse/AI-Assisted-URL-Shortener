package com.example.urlshortener.security;

import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.user.service.OwnerIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentOwnerProvider implements CurrentOwnerProvider {
    @Override
    public OwnerIdentity getCurrentOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new IllegalStateException("Authenticated owner is required");
        }
        return new OwnerIdentity(principal.userId(), principal.email(), principal.role());
    }
}
