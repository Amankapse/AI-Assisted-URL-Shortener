package com.example.urlshortener.auth.service;

import com.example.urlshortener.auth.config.AuthProperties;
import com.example.urlshortener.user.entity.UserEntity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;

    public JwtTokenService(JwtEncoder jwtEncoder, AuthProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public IssuedAccessToken issue(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());
        JwsHeader header = JwsHeader.with(() -> "RS256")
                .type("JWT")
                .keyId(properties.getKeyId())
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("authorities", List.of("ROLE_" + user.getRole().name()))
                .build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(value, expiresAt);
    }

    public record IssuedAccessToken(String value, Instant expiresAt) {
    }
}
