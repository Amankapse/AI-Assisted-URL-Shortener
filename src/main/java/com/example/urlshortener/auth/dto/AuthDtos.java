package com.example.urlshortener.auth.dto;

import com.example.urlshortener.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 128) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password
    ) {
    }

    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {
    }

    public record UserResponse(UUID id, String email, UserRole role) {
    }
}
