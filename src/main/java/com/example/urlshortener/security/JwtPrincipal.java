package com.example.urlshortener.security;

import com.example.urlshortener.user.entity.UserRole;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String email, UserRole role) {
}
