package com.example.urlshortener.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class JwtOwnerAuthenticationToken extends AbstractAuthenticationToken {
    private final JwtPrincipal principal;
    private final Jwt credentials;

    public JwtOwnerAuthenticationToken(JwtPrincipal principal, Collection<? extends GrantedAuthority> authorities, Jwt credentials) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    @Override
    public Jwt getCredentials() {
        return credentials;
    }

    @Override
    public JwtPrincipal getPrincipal() {
        return principal;
    }
}
